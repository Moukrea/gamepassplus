# Game Pass Plus

![Game Pass Plus](android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png "a title")

Multi-platform app to use Xbox Game Pass in its best possible way, leveraging the outstanding work of the folks over [Better xCloud](https://github.com/redphx/better-xcloud)

Most of the credits are to be given to [Better xCloud](https://github.com/redphx/better-xcloud) contributers an Microsoft themselves.

> [!IMPORTANT] 
> [Better xCloud](https://github.com/redphx/better-xcloud) main developper ([redphx](https://github.com/redphx)) has also an released an Android App ([link](https://github.com/redphx/better-xcloud-android)). It's different, but it serves the same purpose. Pick you poison!

## Disclaimer

Game Pass Plus is independently developed and is not affiliated with, authorized, endorsed, or licensed by Microsoft Corporation. "XBOX," "Game Pass," and all related brand are trademarks of Microsoft Corporation. This application utilizes and improves upon the service by integrating scripts from Better xCloud, an independent project aimed at enhancing the user experience of the Game Pass web version. Better xCloud source code is not included in this app but downloaded at runtime.

We acknowledge and appreciate the hard work of the Microsoft Corporation for providing the Game Pass service and the creators behind Better xCloud for their innovative contribution to enhance the experience. All rights and credits for the respective services and improvements go to their rightful owners.

By using Game Pass Plus, you acknowledge the above statements and credit attributions. This application is intended for personal use and aims to respect the rights and contributions of all parties involved.


## Available platforms

- Android
- Android TV

**To be announced (TBA)**:
- Linux, macOS, Windows
- PWA (Android, Linux, macOS, Windows & iOS)

## Features

- Native app feel for the platform of your choice
- All of the features from [Better xCloud](https://github.com/redphx/better-xcloud) ([more info](https://better-xcloud.github.io/features)):
- [Better xCloud](https://github.com/redphx/better-xcloud) auto-update
- UI tweaks so browsing feels more "console-like"

## Known issues

### Some "web browsery" stuff still appear

Yes, the app is based on the web version of [Games Pass](https://www.xbox.com/play), so web "browsery" stuff is to be expected. We're working on it, but please feel free to repport any issues you may encounter!

## Installation

### Android / Android TV

> [!NOTE]
> The `.apk` file is not (and won't ever) be available on the Google Play. There's nothing shady in this app, but I doubt Microsoft would be happy to have a better app than their own on official distribution channels, plus I don't think its worth the hassle. F-Droid is envisageable though, depending on demand.

#### Obtainium (automatic updates, recommended)

[Obtainium](https://github.com/ImranR98/Obtainium) is an app allowing you to get app updates directly from sources.

To add Game Pass Plus to Obtainium, simply start Obtainium, hit the `+` icon, add `https://github.com/Moukrea/gamepassplus` to the source URL field and hit the `Add` button. You'll then only have to install it through the application list within Obtainium.

#### APK download (manual update)

> [!IMPORTANT]
> There won't be app update if you're using the manual installation

Download the latest appropriate `.apk` file from the releases page (from here and nowhere else!). You can download it from the device itself or find your own way to send it there.

Install the `.apk` file like you would install any side loaded application.

Enjoy!

### Linux / Windows / macOS

> This section will be documented once their relative releases are made

## Technicalities

### Android / Android TV

The app is built using Java, not Kotlin.

#### Why is that?

Simply because the nearly 1to1 implementation using Kotlin wasn't working, it had JavaScript/Video playback issues that I couldn't get sorted out. Making the switch to Java solved all my issues.

#### It uses deprecated stuff!

Yes. Well this is my very first Android app and it's a bit of an odd one! Feel free to contribute!

### Linux / Windows / macOS

We are (I am) thinking about building the app using Go with Wails, or a simple Electron app with some React, it all depends on what works or not with minimal effort...

#### Why is that?

Well, it's just convenient! The app requires a browser to work properly as it's just http://www.xbox.com/play opened in a "native looking" window with a bunch of tweaks from [Better xCloud](https://github.com/redphx/better-xcloud) folks. Using WebView based technologies is just convenient as it allows us to build the app for three platforms at once.

### iOS

I have no knowledge in Swift nor own an iPhone and given the difficulty of sideloading apps into iOS devices, I highly doubt there will ever be a native app for iOS. Progressive Web App (PWA) will be the way.

## Credits

- Microsoft
- Better xCloud (redphx and contributors)
- Icons
  - [Cloud computing icons created by Freepik - Flaticon](https://www.flaticon.com/free-icons/cloud-computing)
  - [No internet icons created by Hight Quality Icons - Flaticon](https://www.flaticon.com/free-icons/no-internet)
  - [Error icons created by Freepik - Flaticon](https://www.flaticon.com/free-icons/error)
