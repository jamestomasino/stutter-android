# Stutter for Android

[![Get it on Google Play](https://img.shields.io/badge/Google%20Play-Stutter-34A853?logo=googleplay&logoColor=white)](https://play.google.com/store/apps/details?id=org.tomasino.stutter)
[![Get it on F-Droid](https://img.shields.io/badge/F--Droid-Stutter-1976D2?logo=f-droid&logoColor=white)](https://f-droid.org/packages/org.tomasino.stutter/)
[![Add to Obtainium](https://img.shields.io/badge/Obtainium-Add%20App-3DDC84?logo=android&logoColor=white)](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/jamestomasino/stutter-android)

Stutter is a native Android reading app for [Rapid Serial Visual Presentation](https://en.wikipedia.org/wiki/Rapid_serial_visual_presentation) (RSVP). RSVP is a way to read faster with less eye movement, one word at a time.

_This app is inspired by my [Stutter browser extension](https://www.github.com/jamestomasino/stutter)._

## Privacy first
- No cookies.
- No shared browser state.
- No analytics.
- No tracking.
- No history or content persistence beyond the current session.

If you close the app, your content is gone.

### International support
- ICU is used for word segmentation.
- Language aware hyphenation is used for long words.
- Language selection follows this order:
  1. Language declared in the content.
  2. User configured default language.
  3. Device locale.

## Supported input methods

### Paste text
Paste any plain text directly into the app and start reading.

### Share text
Share selected text from another app to Stutter.

### Share URL
Share a public article URL to Stutter.  
The app will fetch the page and extract the main readable content.

Authenticated or paywalled pages usually cannot be fetched by URL alone.  
In those cases, paste text instead.

## Learn more

- Development, build, and release details live in `DEVELOPMENT.md`.
- Cross-channel release steps are in `RELEASE.md`.

## Research

Stutter attempts to make use of the latest scientific journal data on rapid serial visual presentation. Some of the works that have influenced the project are:

- [Optimizing the reading of electronic text using rapid serial visual presentation](https://www.tandfonline.com/doi/abs/10.1080/01449290110069400)
- [Parafoveal perception during sentence reading? An ERP paradigm using rapid serial visual presentation (RSVP) with flankers](https://onlinelibrary.wiley.com/doi/full/10.1111/j.1469-8986.2010.01082.x)
- [Temporary suppression of visual processing in an RSVP task: an attentional blink?](https://pubmed.ncbi.nlm.nih.gov/1500880/)
- [The more your mind wanders, the smaller your attentional blink: An individual differences study](https://journals.sagepub.com/doi/10.1080/17470218.2014.940985)
- [The Relationship between Reading Strategy Use and Reading Comprehension as Mediated by Reading Rate: The Case of Eye Movement Training by Rapid Serial Visual Presentation (RSVP)](https://eric.ed.gov/?id=EJ1283679)
- [Rapid serial visual presentation in reading: The case of Spritz](https://www.sciencedirect.com/science/article/pii/S0747563214007663)
- [Capturing and holding attention: The impact of emotional words in rapid serial visual presentation](https://link.springer.com/article/10.3758/MC.36.1.182)
- [Perceptual and cognitive factors imposing “speed limits” on reading rate: a study with the rapid serial visual presentation](https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0153786)

## Privacy Policy

Stutter collects no user data. Nothing about your usage is stored or transferred to any server. It can be used offline.

## License

[GPL3](LICENSE)
