package cx.viz.slovo.platform

/**
 * Screen-view analytics → self-hosted Umami (https://analytics.nextgensoft.co,
 * website "SLOVO"). One fire-and-forget POST per screen: no cookies, no
 * identifiers, nothing beyond the screen name plus app/OS version in the
 * User-Agent. Failures are swallowed. Off by default so tests and previews never
 * report; the platform entry points (MainActivity / MainViewController) turn it on.
 */
object Analytics {
    var enabled = false

    fun screen(name: String) {
        if (!enabled) return
        val body = """{"type":"event","payload":{"website":"$WEBSITE_ID","hostname":"$HOSTNAME","url":"/$name","title":"$name"}}"""
        postJson("https://analytics.nextgensoft.co/api/send", body, userAgent)
    }

    private const val WEBSITE_ID = "db3c699c-9de0-4173-a186-34eeec7b5b71"
    private const val HOSTNAME = "cx.viz.slovo"
}

/** Browser-shaped so Umami's bot filter keeps it; carries OS + app version. */
internal expect val userAgent: String

/** Fire-and-forget JSON POST: never throws, never blocks the caller. */
internal expect fun postJson(url: String, body: String, userAgent: String)
