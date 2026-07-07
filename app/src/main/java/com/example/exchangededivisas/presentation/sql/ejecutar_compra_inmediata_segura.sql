create or replace function public.ejecutar_compra_inmediata_segura(
    p_usuarioid integer,
    p_parmonedaid integer,
    p_cantidad numeric
)
returns jsonb
language plpgsql
security definer
as $$
declare
    v_now timestamptz := now();
    v_pair record;
    v_user record;
    v_wallet_id integer;
    v_saldo_origen_id integer;
    v_saldo_destino_id integer;
    v_saldo_origen_before numeric := 0;
    v_saldo_origen_after numeric := 0;
    v_saldo_destino_before numeric := 0;
    v_saldo_destino_after numeric := 0;
    v_total numeric := 0;
    v_covered numeric := 0;
    v_min_price numeric := null;
    v_max_price numeric := null;
    v_avg_price numeric := 0;
    v_operation_id integer;
    v_offer record;
    v_plan record;
    v_take numeric;
    v_subtotal numeric;
    v_new_pending numeric;
    v_new_sold numeric;
    v_new_received numeric;
    v_new_status varchar;
    v_seller_wallet_id integer;
    v_seller_saldo_id integer;
    v_seller_before numeric := 0;
    v_seller_after numeric := 0;
    v_seller_email varchar;
begin
    if p_cantidad is null or p_cantidad <= 0 then
        raise exception 'Valor inválido';
    end if;

    select u.*
    into v_user
    from usuarios u
    where u.usuarioid = p_usuarioid
    for update;

    if not found then
        raise exception 'No se encontró el usuario.';
    end if;

    if lower(coalesce(v_user.estado, '')) <> lower('Activo') then
        raise exception 'Usuario restringido: no puede comprar inmediatamente.';
    end if;

    select
        pm.parmonedaid,
        pm.monedaorigenid,
        pm.monedadestinoid,
        trim(mo.codigoiso) as monedaorigen,
        trim(md.codigoiso) as monedadestino
    into v_pair
    from paresmoneda pm
    join monedas mo on mo.monedaid = pm.monedaorigenid
    join monedas md on md.monedaid = pm.monedadestinoid
    where pm.parmonedaid = p_parmonedaid
      and pm.activo = true;

    if not found then
        raise exception 'No existe el par de monedas seleccionado.';
    end if;

    create temporary table if not exists tmp_plan_compra_segura (
        ofertaventaid integer,
        vendedorid integer,
        cantidad numeric,
        precio numeric,
        subtotal numeric
    ) on commit drop;

    truncate table tmp_plan_compra_segura;

    for v_offer in
        select *
        from ofertasventa
        where parmonedaid = p_parmonedaid
          and usuarioid <> p_usuarioid
          and estado in ('Activa', 'Parcialmente ejecutada')
          and cantidadpendiente > 0
        order by preciounitario asc, fechacreacion asc
        for update
    loop
        exit when v_covered >= p_cantidad;

        v_take := least(p_cantidad - v_covered, v_offer.cantidadpendiente);
        v_subtotal := v_take * v_offer.preciounitario;

        if v_take > 0 then
            insert into tmp_plan_compra_segura(
                ofertaventaid,
                vendedorid,
                cantidad,
                precio,
                subtotal
            ) values (
                v_offer.ofertaventaid,
                v_offer.usuarioid,
                v_take,
                v_offer.preciounitario,
                v_subtotal
            );

            v_covered := v_covered + v_take;
            v_total := v_total + v_subtotal;
            v_min_price := coalesce(least(v_min_price, v_offer.preciounitario), v_offer.preciounitario);
            v_max_price := coalesce(greatest(v_max_price, v_offer.preciounitario), v_offer.preciounitario);
        end if;
    end loop;

    if v_covered < p_cantidad then
        raise exception 'Liquidez insuficiente';
    end if;

    v_avg_price := case when v_covered > 0 then v_total / v_covered else 0 end;

    select billeteraid
    into v_wallet_id
    from billeteras
    where usuarioid = p_usuarioid;

    if v_wallet_id is null then
        insert into billeteras(usuarioid, fechacreacion)
        values (p_usuarioid, v_now)
        returning billeteraid into v_wallet_id;
    end if;

    select saldoid, saldodisponible
    into v_saldo_origen_id, v_saldo_origen_before
    from saldosbilletera
    where billeteraid = v_wallet_id
      and monedaid = v_pair.monedaorigenid
    for update;

    if v_saldo_origen_id is null then
        raise exception 'Saldo insuficiente';
    end if;

    if v_saldo_origen_before < v_total then
        raise exception 'Saldo insuficiente';
    end if;

    select saldoid, saldodisponible
    into v_saldo_destino_id, v_saldo_destino_before
    from saldosbilletera
    where billeteraid = v_wallet_id
      and monedaid = v_pair.monedadestinoid
    for update;

    if v_saldo_destino_id is null then
        insert into saldosbilletera(
            billeteraid,
            monedaid,
            saldodisponible,
            fechaactualizacion
        ) values (
            v_wallet_id,
            v_pair.monedadestinoid,
            0,
            v_now
        ) returning saldoid, saldodisponible
          into v_saldo_destino_id, v_saldo_destino_before;
    end if;

    insert into operacionesinmediatas(
        usuarioid,
        parmonedaid,
        tipooperacion,
        metodoejecucion,
        cantidadsolicitada,
        cantidadejecutada,
        preciominimo,
        preciomaximo,
        preciopromedio,
        totalpagado,
        totalrecibido,
        estado,
        fechaoperacion,
        operacionpadreid
    ) values (
        p_usuarioid,
        p_parmonedaid,
        'Compra inmediata',
        'Normal',
        p_cantidad,
        v_covered,
        v_min_price,
        v_max_price,
        v_avg_price,
        v_total,
        v_covered,
        'Completada',
        v_now,
        null
    ) returning operacioninmediataid into v_operation_id;

    v_saldo_origen_after := v_saldo_origen_before - v_total;
    update saldosbilletera
    set saldodisponible = v_saldo_origen_after,
        fechaactualizacion = v_now
    where saldoid = v_saldo_origen_id;

    insert into movimientosbilletera(
        usuarioid,
        monedaid,
        tipomovimiento,
        monto,
        saldoanterior,
        saldoposterior,
        fechamovimiento,
        referenciatipo,
        referenciaid
    ) values (
        p_usuarioid,
        v_pair.monedaorigenid,
        'CompraInmediata',
        v_total,
        v_saldo_origen_before,
        v_saldo_origen_after,
        v_now,
        'operacionesinmediatas',
        v_operation_id
    );

    v_saldo_destino_after := v_saldo_destino_before + v_covered;
    update saldosbilletera
    set saldodisponible = v_saldo_destino_after,
        fechaactualizacion = v_now
    where saldoid = v_saldo_destino_id;

    insert into movimientosbilletera(
        usuarioid,
        monedaid,
        tipomovimiento,
        monto,
        saldoanterior,
        saldoposterior,
        fechamovimiento,
        referenciatipo,
        referenciaid
    ) values (
        p_usuarioid,
        v_pair.monedadestinoid,
        'CompraInmediata',
        v_covered,
        v_saldo_destino_before,
        v_saldo_destino_after,
        v_now,
        'operacionesinmediatas',
        v_operation_id
    );

    for v_plan in
        select p.*, o.cantidadvendida, o.cantidadpendiente, o.totalrecibido
        from tmp_plan_compra_segura p
        join ofertasventa o on o.ofertaventaid = p.ofertaventaid
        order by p.precio asc, p.ofertaventaid asc
    loop
        v_new_sold := v_plan.cantidadvendida + v_plan.cantidad;
        v_new_pending := greatest(0, v_plan.cantidadpendiente - v_plan.cantidad);
        v_new_received := v_plan.totalrecibido + v_plan.subtotal;
        v_new_status := case
            when v_new_pending <= 0.000001 then 'Completada'
            else 'Parcialmente ejecutada'
        end;

        update ofertasventa
        set cantidadvendida = v_new_sold,
            cantidadpendiente = v_new_pending,
            totalrecibido = v_new_received,
            estado = v_new_status,
            fechaactualizacion = v_now
        where ofertaventaid = v_plan.ofertaventaid;

        select billeteraid
        into v_seller_wallet_id
        from billeteras
        where usuarioid = v_plan.vendedorid;

        if v_seller_wallet_id is null then
            insert into billeteras(usuarioid, fechacreacion)
            values (v_plan.vendedorid, v_now)
            returning billeteraid into v_seller_wallet_id;
        end if;

        select saldoid, saldodisponible
        into v_seller_saldo_id, v_seller_before
        from saldosbilletera
        where billeteraid = v_seller_wallet_id
          and monedaid = v_pair.monedaorigenid
        for update;

        if v_seller_saldo_id is null then
            insert into saldosbilletera(
                billeteraid,
                monedaid,
                saldodisponible,
                fechaactualizacion
            ) values (
                v_seller_wallet_id,
                v_pair.monedaorigenid,
                0,
                v_now
            ) returning saldoid, saldodisponible
              into v_seller_saldo_id, v_seller_before;
        end if;

        v_seller_after := v_seller_before + v_plan.subtotal;

        update saldosbilletera
        set saldodisponible = v_seller_after,
            fechaactualizacion = v_now
        where saldoid = v_seller_saldo_id;

        insert into movimientosbilletera(
            usuarioid,
            monedaid,
            tipomovimiento,
            monto,
            saldoanterior,
            saldoposterior,
            fechamovimiento,
            referenciatipo,
            referenciaid
        ) values (
            v_plan.vendedorid,
            v_pair.monedaorigenid,
            'VentaInmediata',
            v_plan.subtotal,
            v_seller_before,
            v_seller_after,
            v_now,
            'operacionesinmediatas',
            v_operation_id
        );

        insert into historialtransacciones(
            usuarioid,
            tipooperacion,
            referenciaid,
            parmonedaid,
            monedaid,
            fechahora,
            estado,
            metodoejecucion
        ) values (
            v_plan.vendedorid,
            'Oferta de venta',
            v_plan.ofertaventaid,
            p_parmonedaid,
            null,
            v_now,
            v_new_status,
            case when v_new_status = 'Completada' then 'Ejecución total' else 'Ejecución parcial' end
        );

        insert into historialtransacciones(
            usuarioid,
            tipooperacion,
            referenciaid,
            parmonedaid,
            monedaid,
            fechahora,
            estado,
            metodoejecucion
        ) values (
            v_plan.vendedorid,
            'Venta inmediata',
            v_operation_id,
            p_parmonedaid,
            null,
            v_now,
            'Completada',
            'Normal'
        );

        select correoelectronico
        into v_seller_email
        from usuarios
        where usuarioid = v_plan.vendedorid;

        insert into notificacionescorreo(
            usuarioid,
            tiponotificacionid,
            correodestino,
            tipoevento,
            asunto,
            cuerpo,
            estadoenvio,
            fechacreacion,
            fechaenvio,
            referenciatipo,
            referenciaid
        ) values (
            v_plan.vendedorid,
            null,
            coalesce(v_seller_email, 'correo@pendiente.com'),
            'PROGRESO_OFERTA',
            'Tu oferta recibió una compra',
            'Se ejecutaron ' || v_plan.cantidad || ' ' || v_pair.monedadestino ||
            ' de tu oferta. Recibiste ' || v_plan.subtotal || ' ' || v_pair.monedaorigen || '.',
            'Pendiente',
            v_now,
            null,
            'operacionesinmediatas',
            v_operation_id
        );
    end loop;

    insert into historialtransacciones(
        usuarioid,
        tipooperacion,
        referenciaid,
        parmonedaid,
        monedaid,
        fechahora,
        estado,
        metodoejecucion
    ) values (
        p_usuarioid,
        'Compra inmediata',
        v_operation_id,
        p_parmonedaid,
        null,
        v_now,
        'Completada',
        'Normal'
    );

    return jsonb_build_object(
        'operacionid', v_operation_id,
        'cantidadcomprada', v_covered,
        'totalpagado', v_total,
        'monedaorigen', v_pair.monedaorigen,
        'monedadestino', v_pair.monedadestino,
        'mensaje', 'Compra inmediata ejecutada correctamente.'
    );
end;
$$;
