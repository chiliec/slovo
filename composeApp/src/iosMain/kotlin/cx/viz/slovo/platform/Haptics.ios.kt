package cx.viz.slovo.platform

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

class IosHaptics : Haptics {
    override fun light() {
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight).impactOccurred()
    }

    override fun success() {
        UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
    }

    override fun error() { /* none on error, per design spec */ }
}
