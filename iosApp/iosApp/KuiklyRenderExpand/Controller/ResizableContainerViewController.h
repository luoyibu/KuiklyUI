#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

/// Container VC that hosts a KuiklyRenderViewController in a resizable child container.
/// Used to simulate iPad 26 window drag/resize to reproduce VForLazy blank issue.
@interface ResizableContainerViewController : UIViewController

@end

NS_ASSUME_NONNULL_END
