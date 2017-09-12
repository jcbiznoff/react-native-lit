
#import "RNLit.h"
#import <AVFoundation/AVFoundation.h>

@interface RNLit ()
@end

@implementation RNLit


- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}
RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(isFlashAvail:(RCTPromiseResolveBlock) resolve
                  rejecter:(RCTPromiseRejectBlock) reject) {
    AVCaptureDevice *device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    if ([device hasTorch]) {
        resolve(@{@"deviceSupportsFlash": @(YES)});
    } else {
        resolve(@{@"deviceSupportsFlash": @(NO)});
    }
}

RCT_EXPORT_METHOD(turnOn: (BOOL) turnOnNow
                  resolver:(RCTPromiseResolveBlock) resolve
                  rejecter:(RCTPromiseRejectBlock) reject) {
    AVCaptureDevice *device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    if ([device hasTorch]) {
        [device lockForConfiguration:nil];
        [device setTorchMode: turnOnNow ? AVCaptureTorchModeOn : AVCaptureTorchModeOff];
        [device unlockForConfiguration];
    }
    resolve(@{@"isEnabled": @(turnOnNow)});
}

@end
