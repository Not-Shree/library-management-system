import { Capacitor } from '@capacitor/core';

/** True when running inside the Android app (Capacitor), false in a normal browser. */
export const isNativeApp = Capacitor.isNativePlatform();
