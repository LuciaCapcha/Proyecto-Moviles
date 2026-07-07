import { serve } from "https://deno.land/std@0.224.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const RESEND_API_KEY = Deno.env.get("RESEND_API_KEY")
const EMAIL_FROM = Deno.env.get("EMAIL_FROM") ?? "Ezchange <onboarding@resend.dev>"
const SUPABASE_URL =
  Deno.env.get("PROJECT_URL") ??
  Deno.env.get("SUPABASE_URL")

const SUPABASE_SERVICE_ROLE_KEY =
  Deno.env.get("SERVICE_ROLE_KEY") ??
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")

serve(async (_req) => {
  if (!RESEND_API_KEY || !SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) {
    return new Response(
      JSON.stringify({
        ok: false,
        error: "Faltan variables de entorno."
      }),
      {
        status: 500,
        headers: { "Content-Type": "application/json" }
      }
    )
  }

  const supabase = createClient(
    SUPABASE_URL,
    SUPABASE_SERVICE_ROLE_KEY
  )

  const { data: emails, error: selectError } = await supabase
    .from("notificacionescorreo")
    .select("*")
    .eq("estadoenvio", "Pendiente")
    .order("fechacreacion", { ascending: true })
    .limit(20)

  if (selectError) {
    return new Response(
      JSON.stringify({
        ok: false,
        error: selectError.message
      }),
      {
        status: 500,
        headers: { "Content-Type": "application/json" }
      }
    )
  }

  const results = []

  for (const email of emails ?? []) {
    try {
      const html = buildEmailHtml({
        asunto: email.asunto,
        cuerpo: email.cuerpo,
        tipoEvento: email.tipoevento
      })

      const resendResponse = await fetch("https://api.resend.com/emails", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${RESEND_API_KEY}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          from: EMAIL_FROM,
          to: email.correodestino,
          subject: email.asunto,
          html: html,
          text: email.cuerpo
        })
      })

      const resendBody = await resendResponse.json().catch(() => ({}))

      if (!resendResponse.ok) {
        await supabase
          .from("notificacionescorreo")
          .update({
            estadoenvio: "Error",
            fechaenvio: new Date().toISOString()
          })
          .eq("notificacionid", email.notificacionid)

        results.push({
          notificacionid: email.notificacionid,
          status: "Error",
          detail: resendBody
        })

        continue
      }

      await supabase
        .from("notificacionescorreo")
        .update({
          estadoenvio: "Enviado",
          fechaenvio: new Date().toISOString()
        })
        .eq("notificacionid", email.notificacionid)

      results.push({
        notificacionid: email.notificacionid,
        status: "Enviado",
        providerId: resendBody.id ?? null
      })
    } catch (error) {
      await supabase
        .from("notificacionescorreo")
        .update({
          estadoenvio: "Error",
          fechaenvio: new Date().toISOString()
        })
        .eq("notificacionid", email.notificacionid)

      results.push({
        notificacionid: email.notificacionid,
        status: "Error",
        detail: String(error)
      })
    }
  }

  return new Response(
    JSON.stringify({
      ok: true,
      processed: results.length,
      results
    }),
    {
      status: 200,
      headers: { "Content-Type": "application/json" }
    }
  )
})

function buildEmailHtml(params: {
  asunto: string
  cuerpo: string
  tipoEvento: string
}) {
  return `
    <div style="font-family: Arial, sans-serif; max-width: 620px; margin: 0 auto; padding: 24px; color: #1f2937;">
      <div style="background: #111827; color: white; padding: 18px 22px; border-radius: 12px 12px 0 0;">
        <h2 style="margin: 0;">Ezchange</h2>
        <p style="margin: 6px 0 0 0; color: #d1d5db;">${escapeHtml(params.tipoEvento)}</p>
      </div>

      <div style="border: 1px solid #e5e7eb; border-top: 0; padding: 22px; border-radius: 0 0 12px 12px;">
        <h3 style="margin-top: 0;">${escapeHtml(params.asunto)}</h3>
        <p style="line-height: 1.55; white-space: pre-line;">${escapeHtml(params.cuerpo)}</p>

        <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 22px 0;" />

        <p style="font-size: 12px; color: #6b7280;">
          Este correo fue generado automáticamente por Ezchange.
        </p>
      </div>
    </div>
  `
}

function escapeHtml(value: string) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;")
}