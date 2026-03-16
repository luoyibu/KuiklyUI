#import "ResizableContainerViewController.h"
#import "KuiklyRenderViewController.h"

/// Drag handle size
static const CGFloat kHandleSize = 44.0;
static const CGFloat kMinContainerW = 250.0;
static const CGFloat kMinContainerH = 350.0;

@interface ResizableContainerViewController ()

@property (nonatomic, strong) UINavigationController *childNavController;
@property (nonatomic, strong) UIView *containerView;
@property (nonatomic, strong) UIView *dragHandle;       // bottom-right drag handle
@property (nonatomic, strong) UIView *dragHandleRight;   // right-edge drag handle
@property (nonatomic, strong) UIView *dragHandleBottom;  // bottom-edge drag handle
@property (nonatomic, strong) UILabel *sizeLabel;
@property (nonatomic, strong) UIButton *resetButton;

// Container geometry (in points)
@property (nonatomic, assign) CGFloat containerW;
@property (nonatomic, assign) CGFloat containerH;
@property (nonatomic, assign) CGFloat containerTop;

// Drag state
@property (nonatomic, assign) CGPoint dragStartPoint;
@property (nonatomic, assign) CGFloat dragStartW;
@property (nonatomic, assign) CGFloat dragStartH;

@end

@implementation ResizableContainerViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = [UIColor systemGroupedBackgroundColor];

    // Initial small window size
    _containerW = 400;
    _containerH = 600;

    // --- Size label ---
    self.sizeLabel = [[UILabel alloc] init];
    self.sizeLabel.font = [UIFont monospacedSystemFontOfSize:13 weight:UIFontWeightMedium];
    self.sizeLabel.textColor = [UIColor secondaryLabelColor];
    self.sizeLabel.textAlignment = NSTextAlignmentLeft;
    [self.view addSubview:self.sizeLabel];

    // --- Reset button ---
    self.resetButton = [UIButton buttonWithType:UIButtonTypeSystem];
    [self.resetButton setTitle:@"Reset (400x600)" forState:UIControlStateNormal];
    self.resetButton.titleLabel.font = [UIFont systemFontOfSize:14 weight:UIFontWeightMedium];
    [self.resetButton setTitleColor:[UIColor whiteColor] forState:UIControlStateNormal];
    self.resetButton.backgroundColor = [UIColor systemBlueColor];
    self.resetButton.layer.cornerRadius = 16;
    [self.resetButton addTarget:self action:@selector(resetSize) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:self.resetButton];

    // --- Container view ---
    self.containerView = [[UIView alloc] init];
    self.containerView.backgroundColor = [UIColor whiteColor];
    self.containerView.layer.cornerRadius = 12;
    self.containerView.layer.shadowColor = [UIColor blackColor].CGColor;
    self.containerView.layer.shadowOpacity = 0.15;
    self.containerView.layer.shadowOffset = CGSizeMake(0, 2);
    self.containerView.layer.shadowRadius = 10;
    self.containerView.clipsToBounds = YES;
    [self.view addSubview:self.containerView];

    // --- Kuikly page ---
    KuiklyRenderViewController *kuiklyVC = [[KuiklyRenderViewController alloc]
        initWithPageName:@"VforLazyDragIssue" pageData:@{}];
    self.childNavController = [[UINavigationController alloc] initWithRootViewController:kuiklyVC];
    self.childNavController.navigationBarHidden = YES;
    [self addChildViewController:self.childNavController];
    [self.containerView addSubview:self.childNavController.view];
    [self.childNavController didMoveToParentViewController:self];

    // --- Drag handles ---
    // Bottom-right corner handle (diagonal resize)
    self.dragHandle = [self createDragHandleWithColor:[UIColor systemBlueColor]];
    UIPanGestureRecognizer *cornerPan = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(handleCornerDrag:)];
    [self.dragHandle addGestureRecognizer:cornerPan];
    [self.view addSubview:self.dragHandle];

    // Right edge handle (horizontal resize)
    self.dragHandleRight = [self createEdgeHandleVertical:YES color:[UIColor systemIndigoColor]];
    UIPanGestureRecognizer *rightPan = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(handleRightDrag:)];
    [self.dragHandleRight addGestureRecognizer:rightPan];
    [self.view addSubview:self.dragHandleRight];

    // Bottom edge handle (vertical resize)
    self.dragHandleBottom = [self createEdgeHandleVertical:NO color:[UIColor systemIndigoColor]];
    UIPanGestureRecognizer *bottomPan = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(handleBottomDrag:)];
    [self.dragHandleBottom addGestureRecognizer:bottomPan];
    [self.view addSubview:self.dragHandleBottom];

    [self layoutAll];
}

#pragma mark - Handle creation helpers

- (UIView *)createDragHandleWithColor:(UIColor *)color {
    UIView *handle = [[UIView alloc] initWithFrame:CGRectMake(0, 0, kHandleSize, kHandleSize)];
    handle.backgroundColor = color;
    handle.layer.cornerRadius = kHandleSize / 2;

    // Arrow icon
    UILabel *arrow = [[UILabel alloc] initWithFrame:handle.bounds];
    arrow.text = @"⤡";
    arrow.font = [UIFont systemFontOfSize:20];
    arrow.textColor = [UIColor whiteColor];
    arrow.textAlignment = NSTextAlignmentCenter;
    [handle addSubview:arrow];
    return handle;
}

- (UIView *)createEdgeHandleVertical:(BOOL)vertical color:(UIColor *)color {
    CGFloat w = vertical ? 12 : 60;
    CGFloat h = vertical ? 60 : 12;
    UIView *handle = [[UIView alloc] initWithFrame:CGRectMake(0, 0, w, h)];
    handle.backgroundColor = color;
    handle.layer.cornerRadius = 6;
    handle.alpha = 0.7;
    return handle;
}

#pragma mark - Drag gestures

- (void)handleCornerDrag:(UIPanGestureRecognizer *)pan {
    if (pan.state == UIGestureRecognizerStateBegan) {
        self.dragStartPoint = [pan locationInView:self.view];
        self.dragStartW = self.containerW;
        self.dragStartH = self.containerH;
    } else if (pan.state == UIGestureRecognizerStateChanged) {
        CGPoint current = [pan locationInView:self.view];
        CGFloat dw = current.x - self.dragStartPoint.x;
        CGFloat dh = current.y - self.dragStartPoint.y;
        [self resizeToWidth:self.dragStartW + dw height:self.dragStartH + dh];
    }
}

- (void)handleRightDrag:(UIPanGestureRecognizer *)pan {
    if (pan.state == UIGestureRecognizerStateBegan) {
        self.dragStartPoint = [pan locationInView:self.view];
        self.dragStartW = self.containerW;
    } else if (pan.state == UIGestureRecognizerStateChanged) {
        CGPoint current = [pan locationInView:self.view];
        CGFloat dw = current.x - self.dragStartPoint.x;
        [self resizeToWidth:self.dragStartW + dw height:self.containerH];
    }
}

- (void)handleBottomDrag:(UIPanGestureRecognizer *)pan {
    if (pan.state == UIGestureRecognizerStateBegan) {
        self.dragStartPoint = [pan locationInView:self.view];
        self.dragStartH = self.containerH;
    } else if (pan.state == UIGestureRecognizerStateChanged) {
        CGPoint current = [pan locationInView:self.view];
        CGFloat dh = current.y - self.dragStartPoint.y;
        [self resizeToWidth:self.containerW height:self.dragStartH + dh];
    }
}

- (void)resizeToWidth:(CGFloat)w height:(CGFloat)h {
    CGFloat screenW = self.view.bounds.size.width;
    CGFloat screenH = self.view.bounds.size.height;
    CGFloat maxW = screenW - 20;
    CGFloat maxH = screenH - self.containerTop - 10;

    self.containerW = MAX(kMinContainerW, MIN(w, maxW));
    self.containerH = MAX(kMinContainerH, MIN(h, maxH));
    [self layoutAll];
}

#pragma mark - Reset

- (void)resetSize {
    self.containerW = 400;
    self.containerH = 600;
    [UIView animateWithDuration:0.25 animations:^{
        [self layoutAll];
    }];
}

#pragma mark - Layout

- (void)layoutAll {
    CGFloat screenW = self.view.bounds.size.width;
    CGFloat safeTop = self.view.safeAreaInsets.top ?: 50;

    // Top bar
    CGFloat barY = safeTop + 8;
    self.sizeLabel.frame = CGRectMake(16, barY, 200, 32);
    self.resetButton.frame = CGRectMake(screenW - 170, barY, 154, 32);

    // Container
    self.containerTop = barY + 40;
    CGFloat containerX = (screenW - self.containerW) / 2;
    self.containerView.frame = CGRectMake(containerX, self.containerTop, self.containerW, self.containerH);
    self.childNavController.view.frame = self.containerView.bounds;

    // Drag handles
    CGFloat cx = containerX + self.containerW;
    CGFloat cy = self.containerTop + self.containerH;

    // Corner handle (bottom-right)
    self.dragHandle.center = CGPointMake(cx, cy);

    // Right edge handle (middle of right edge)
    self.dragHandleRight.center = CGPointMake(cx + 10, self.containerTop + self.containerH / 2);

    // Bottom edge handle (middle of bottom edge)
    self.dragHandleBottom.center = CGPointMake(containerX + self.containerW / 2, cy + 10);

    // Size label
    self.sizeLabel.text = [NSString stringWithFormat:@"%.0f × %.0f", self.containerW, self.containerH];
}

- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    [self layoutAll];
}

- (void)viewSafeAreaInsetsDidChange {
    [super viewSafeAreaInsetsDidChange];
    [self layoutAll];
}

@end
