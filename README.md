[working version patch]
  ~
a self contained network monitoring, mobile [android] application build. including real-time, app specific, display capture, of both mobile data, and WiFi internet traffic.., idk, maybe it'll help you identify a leak.

this app is not compatible with any of your active, VPN, security measures; since it relies on the VPNservice to deliver. but.., if you cannot afford to lower those security measures. then i have the solution!
for now, you'll have to contact me, and we'll work out an exchange. [soon enough tho'

in the meantime, yous can just download the standard vpnService, 'ntG-Dv3-3.apk' from the files section here.
[the source-code is also laying around here somewhere; i think. xD lol]
meaning; you can check it – to make sure there's nothing devious going on. and, if you're adventurous, you can try building your own custom package version.

;)

______

ᵃ ˡⁱᵍʰᵗʷᵉⁱᵍʰᵗ, ʰⁱᵍʰ-ᵖᵉʳᶠᵒʳᵐᵃⁿᶜᵉ, ᵃⁿᵈʳᵒⁱᵈ ⁿᵉᵗʷᵒʳᵏ ᵗʳᵃᶠᶠⁱᶜ ᵐᵒⁿⁱᵗᵒʳⁱⁿᵍ ᵃᵖᵖ – ᵇᵘⁱˡᵗ ʷⁱᵗʰ ᵏᵒᵗˡⁱⁿ, ˢᵗᵃⁿᵈᵃʳᵈ ᵃⁿᵈʳᵒⁱᵈ ˢᵈᵏ ᵃᵖⁱˢ, ᵃⁿᵈ ᵃ ˡᵒᶜᵃˡ `ᵛᵖⁿˢᵉʳᵛⁱᶜᵉ` ᵖᵃᶜᵏᵉᵗ ⁱⁿˢᵖᵉᶜᵗⁱᵒⁿ ˡᵃʸᵉʳ.

## Key Features
* **Local `VpnService` Interceptor:** Creates a virtual TUN interface on `10.0.0.2` to capture real-time IP packet metrics.
* **Non-Blocking Packet Forwarding:** Bypasses local routing blocks (`allowBypass()`) to retain full, uninterrupted internet access during individual application inspection.
* **RAM Persistence:** Application list is cached in `NetworkMonitorApplication` for instant UI rendering across activity switches.
* **Dynamic Canvas Graphing:** Custom native canvas `GraphView.kt` rendering smoothed cubic Bézier speed lines with zero allocation during `onDraw()`.

## Build Specifications
* **Target SDK:** 33 (Android 13)
* **Minimum required SDK:** 26 (Android 8.0)
* **JDK:** OpenJDK 17
* **Gradle:** 8.2 Standalone
