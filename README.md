# Organizer

Minimaler Aufgaben-Organizer im Nothing-Stil (schwarz, dunkelrot, Punkt-Schrift) mit To-Do-Widget für den Homescreen.

## APK herunterladen

Jeder Push auf `main` baut die App automatisch (GitHub Actions). Die fertige **Organizer.apk** liegt danach unter **Releases** → neueste Version.

Auf dem Handy: APK öffnen → „Aus dieser Quelle installieren“ erlauben → Installieren.
Neue Versionen lassen sich über die alte installieren, die Aufgaben bleiben erhalten.

## Funktionen

- **Heute**: alle offenen Aufgaben, nach Wichtigkeit × Dringlichkeit sortiert, Abhaken mit Animation
- **Baum**: Ordner/Projekte mit Unteraufgaben
- **Aufgabe**: Start, Fällig, Uhrzeit, Wiederholung, Erinnerung, Wichtigkeit, Dringlichkeit, Kontexte, Flag, Notizen
- **Filter**: Ansichten (Inbox, Markiert, Wichtig & dringend, Wiederkehrend, Erledigt), Kontexte, Flags
- **Alarme**: echte Android-Benachrichtigungen mit „Erledigt“-Knopf
- **Schnelleingabe**: `Kunde anrufen @Telefon morgen 14:00 !`
- **Widget (4×2)**: Kreis tippen = abhaken, Aufgabe tippen = Schnellaktionen in der App

## Aufbau

- `app/src/main/assets/index.html` – die komplette Oberfläche (HTML/CSS/JS), läuft in einer WebView
- `Store.kt` – gemeinsame Daten für App, Widget und Erinnerungen
- `TodoWidget.kt`, `WidgetService.kt`, `WidgetActionActivity.kt` – das Homescreen-Widget
- `Reminders.kt` – Erinnerungen und Benachrichtigungen

Hinweis: Der Signatur-Schlüssel `app/organizer.jks` liegt bewusst im (privaten) Repo, damit Updates sich drüberinstallieren lassen. Nicht für den Play Store gedacht.
