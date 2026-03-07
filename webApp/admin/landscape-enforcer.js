/**
 * Interactive Landscape Orientation Enforcer with Screen Lock API
 * 
 * Senior Developer Approach - Enhanced Interactive Version:
 * 1. Detect device type and platform (iOS/Android)
 * 2. Use Screen Orientation Lock API (Android Chrome)
 * 3. Provide engaging, interactive UI with animations
 * 4. Fullscreen mode for immersive experience
 * 5. Graceful fallback for unsupported browsers (iOS)
 * 6. Real-time orientation monitoring
 * 7. Smooth transitions and micro-interactions
 */

(function() {
  'use strict';

  // ═══════════════════════════════════════════════════════════════════
  // Configuration
  // ═══════════════════════════════════════════════════════════════════
  
  const CONFIG = {
    // Devices with width <= this will be considered mobile/tablet
    maxTabletWidth: 1024,
    
    // Minimum aspect ratio to be considered landscape (width/height)
    landscapeAspectRatio: 1.0,
    
    // Animation duration for overlay
    animationDuration: 400,
    
    // Enable debug logging
    debug: false,
    
    // Enable fullscreen mode when locking orientation
    enableFullscreen: true,
    
    // Auto-hide overlay after successful lock (Android only)
    autoHideAfterLock: true
  };

  // ═══════════════════════════════════════════════════════════════════
  // Device Detection
  // ═══════════════════════════════════════════════════════════════════
  
  function isMobileOrTablet() {
    const userAgent = navigator.userAgent || navigator.vendor || window.opera;
    const width = window.innerWidth;
    
    // Check user agent for mobile/tablet
    const isMobileUA = /android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini/i.test(userAgent.toLowerCase());
    
    // Check screen width
    const isSmallScreen = width <= CONFIG.maxTabletWidth;
    
    // Check touch capability
    const isTouchDevice = ('ontouchstart' in window) || (navigator.maxTouchPoints > 0);
    
    const result = isMobileUA || (isSmallScreen && isTouchDevice);
    
    if (CONFIG.debug) {
      console.log('📱 Device Detection:', {
        userAgent: isMobileUA,
        screenWidth: width,
        isSmallScreen,
        isTouchDevice,
        isMobileOrTablet: result
      });
    }
    
    return result;
  }
  
  // ═══════════════════════════════════════════════════════════════════
  // Platform Detection (iOS vs Android)
  // ═══════════════════════════════════════════════════════════════════
  
  function getPlatform() {
    const userAgent = navigator.userAgent || navigator.vendor || window.opera;
    
    if (/iPad|iPhone|iPod/.test(userAgent) && !window.MSStream) {
      return 'ios';
    }
    
    if (/android/i.test(userAgent)) {
      return 'android';
    }
    
    return 'other';
  }
  
  // ═══════════════════════════════════════════════════════════════════
  // Screen Orientation Lock API Support Detection
  // ═══════════════════════════════════════════════════════════════════
  
  function supportsOrientationLock() {
    return !!(screen.orientation && screen.orientation.lock);
  }
  
  function supportsFullscreen() {
    return !!(
      document.fullscreenEnabled ||
      document.webkitFullscreenEnabled ||
      document.mozFullScreenEnabled ||
      document.msFullscreenEnabled
    );
  }

  // ═══════════════════════════════════════════════════════════════════
  // Orientation Detection
  // ═══════════════════════════════════════════════════════════════════
  
  function isPortraitMode() {
    const width = window.innerWidth;
    const height = window.innerHeight;
    const aspectRatio = width / height;
    
    // Check screen orientation API if available
    const orientationAPI = screen.orientation || screen.mozOrientation || screen.msOrientation;
    let isPortraitAPI = false;
    
    if (orientationAPI) {
      const type = orientationAPI.type || orientationAPI;
      isPortraitAPI = type.includes('portrait');
    }
    
    // Fallback to aspect ratio
    const isPortraitAspect = aspectRatio < CONFIG.landscapeAspectRatio;
    
    const result = isPortraitAPI || isPortraitAspect;
    
    if (CONFIG.debug) {
      console.log('🔄 Orientation Check:', {
        width,
        height,
        aspectRatio: aspectRatio.toFixed(2),
        orientationAPI: orientationAPI ? (orientationAPI.type || orientationAPI) : 'N/A',
        isPortrait: result
      });
    }
    
    return result;
  }

  // ═══════════════════════════════════════════════════════════════════
  // Fullscreen Management
  // ═══════════════════════════════════════════════════════════════════
  
  async function enterFullscreen() {
    if (!CONFIG.enableFullscreen || !supportsFullscreen()) {
      return false;
    }
    
    try {
      const elem = document.documentElement;
      
      if (elem.requestFullscreen) {
        await elem.requestFullscreen();
      } else if (elem.webkitRequestFullscreen) {
        await elem.webkitRequestFullscreen();
      } else if (elem.mozRequestFullScreen) {
        await elem.mozRequestFullScreen();
      } else if (elem.msRequestFullscreen) {
        await elem.msRequestFullscreen();
      }
      
      if (CONFIG.debug) console.log('✅ Entered fullscreen mode');
      return true;
    } catch (error) {
      if (CONFIG.debug) console.log('❌ Failed to enter fullscreen:', error);
      return false;
    }
  }
  
  async function lockOrientationToLandscape() {
    if (!supportsOrientationLock()) {
      if (CONFIG.debug) console.log('❌ Orientation Lock API not supported');
      return false;
    }
    
    try {
      await screen.orientation.lock('landscape');
      if (CONFIG.debug) console.log('🔒 Orientation locked to landscape');
      return true;
    } catch (error) {
      if (CONFIG.debug) console.log('❌ Failed to lock orientation:', error);
      return false;
    }
  }
  
  // ═══════════════════════════════════════════════════════════════════
  // Interactive Overlay Creation
  // ═══════════════════════════════════════════════════════════════════
  
  function createOrientationOverlay() {
    const platform = getPlatform();
    const canLockOrientation = supportsOrientationLock();
    
    const overlay = document.createElement('div');
    overlay.id = 'landscape-enforcer-overlay';
    
    // Platform-specific content
    if (platform === 'android' && canLockOrientation) {
      // Android with Screen Lock API support - Interactive button
      overlay.innerHTML = `
        <div class="landscape-enforcer-content">
          <h2 class="landscape-enforcer-title slide-in">Experience Landscape Mode</h2>
          <p class="landscape-enforcer-message slide-in-delayed">
            Get the full admin dashboard experience in landscape orientation.
          </p>
          
          <div class="device-rotation-visual">
            <div class="device-frame portrait-device">
              <div class="device-screen">
                <div class="screen-content">
                  <div class="screen-line"></div>
                  <div class="screen-line"></div>
                  <div class="screen-line"></div>
                </div>
              </div>
              <div class="device-button"></div>
            </div>
            
            <div class="rotation-arrow">
              <svg width="80" height="80" viewBox="0 0 80 80" fill="white">
                <path d="M40 20 L60 40 L40 60 M20 40 L60 40" stroke="white" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
              </svg>
            </div>
            
            <div class="device-frame landscape-device">
              <div class="device-screen">
                <div class="screen-content">
                  <div class="screen-line wide"></div>
                  <div class="screen-line wide"></div>
                  <div class="screen-line wide"></div>
                </div>
              </div>
              <div class="device-button"></div>
            </div>
          </div>
          
          <button id="lock-orientation-btn" class="lock-orientation-button">
            <span class="button-icon">🔓</span>
            <span class="button-text">Tap to Switch to Landscape</span>
            <span class="button-shine"></span>
          </button>
          
          <p class="landscape-enforcer-hint">
            <span class="hint-icon">✨</span>
            This will enable fullscreen and lock to landscape mode
          </p>
        </div>
      `;
    } else if (platform === 'ios') {
      // iOS - Manual rotation required
      overlay.innerHTML = `
        <div class="landscape-enforcer-content">
          <h2 class="landscape-enforcer-title slide-in">Rotate to Landscape</h2>
          <p class="landscape-enforcer-message slide-in-delayed">
            Please rotate your device to landscape mode for the best admin experience.
          </p>
          
          <div class="device-rotation-visual">
            <div class="device-frame portrait-device">
              <div class="device-screen">
                <div class="screen-content">
                  <div class="screen-line"></div>
                  <div class="screen-line"></div>
                  <div class="screen-line"></div>
                </div>
              </div>
              <div class="device-button"></div>
            </div>
            
            <div class="rotation-arrow">
              <svg width="80" height="80" viewBox="0 0 80 80" fill="white">
                <path d="M40 20 L60 40 L40 60 M20 40 L60 40" stroke="white" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
              </svg>
            </div>
            
            <div class="device-frame landscape-device">
              <div class="device-screen">
                <div class="screen-content">
                  <div class="screen-line wide"></div>
                  <div class="screen-line wide"></div>
                  <div class="screen-line wide"></div>
                </div>
              </div>
              <div class="device-button"></div>
            </div>
          </div>
          
          <div class="ios-instruction-box">
            <div class="instruction-step">
              <span class="step-number">1</span>
              <span class="step-text">Hold your device horizontally</span>
            </div>
            <div class="instruction-step">
              <span class="step-number">2</span>
              <span class="step-text">Turn off rotation lock if enabled</span>
            </div>
            <div class="instruction-step">
              <span class="step-number">3</span>
              <span class="step-text">Enjoy the full dashboard!</span>
            </div>
          </div>
        </div>
      `;
    } else {
      // Fallback for other devices
      overlay.innerHTML = `
        <div class="landscape-enforcer-content">
          <div class="landscape-enforcer-icon-wrapper">
            <div class="landscape-enforcer-icon">
              <svg width="100" height="100" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="15" y="30" width="70" height="40" rx="4" stroke="currentColor" stroke-width="3" fill="none"/>
                <circle cx="25" cy="50" r="2.5" fill="currentColor"/>
              </svg>
            </div>
          </div>
          
          <h2 class="landscape-enforcer-title">Landscape Mode Required</h2>
          <p class="landscape-enforcer-message">
            Please rotate your device to landscape orientation to access the admin dashboard.
          </p>
          
          <div class="landscape-enforcer-animation">
            <div class="device-animation">
              <div class="device-icon portrait">📱</div>
              <div class="arrow-icon">→</div>
              <div class="device-icon landscape">📱</div>
            </div>
          </div>
        </div>
      `;
    }
    
    return overlay;
  }

  // ═══════════════════════════════════════════════════════════════════
  // Overlay Management
  // ═══════════════════════════════════════════════════════════════════
  
  let overlay = null;
  let orientationLocked = false;
  
  function showOrientationWarning() {
    if (overlay) return; // Already showing
    
    overlay = createOrientationOverlay();
    document.body.appendChild(overlay);
    
    // Trigger animation
    requestAnimationFrame(() => {
      overlay.classList.add('visible');
    });
    
    // Setup button click handler for Android
    const lockBtn = document.getElementById('lock-orientation-btn');
    if (lockBtn) {
      lockBtn.addEventListener('click', handleLockOrientationClick);
    }
    
    if (CONFIG.debug) console.log('⚠️ Showing landscape warning');
  }
  
  function hideOrientationWarning() {
    if (!overlay) return; // Not showing
    
    overlay.classList.remove('visible');
    
    setTimeout(() => {
      if (overlay && overlay.parentNode) {
        overlay.parentNode.removeChild(overlay);
      }
      overlay = null;
    }, CONFIG.animationDuration);
    
    if (CONFIG.debug) console.log('✅ Hiding landscape warning');
  }
  
  // ═══════════════════════════════════════════════════════════════════
  // Interactive Button Handler (Android)
  // ═══════════════════════════════════════════════════════════════════
  
  async function handleLockOrientationClick(event) {
    const button = event.currentTarget;
    
    // Add loading state
    button.classList.add('loading');
    button.disabled = true;
    
    const buttonText = button.querySelector('.button-text');
    const buttonIcon = button.querySelector('.button-icon');
    
    if (buttonText) buttonText.textContent = 'Switching to Landscape...';
    if (buttonIcon) buttonIcon.textContent = '⏳';
    
    try {
      // Step 1: Enter fullscreen
      const fullscreenSuccess = await enterFullscreen();
      
      if (fullscreenSuccess) {
        // Small delay for smooth transition
        await new Promise(resolve => setTimeout(resolve, 300));
        
        // Step 2: Lock orientation
        const lockSuccess = await lockOrientationToLandscape();
        
        if (lockSuccess) {
          orientationLocked = true;
          
          // Update button to success state
          if (buttonText) buttonText.textContent = 'Landscape Mode Activated! 🎉';
          if (buttonIcon) buttonIcon.textContent = '🔒';
          button.classList.remove('loading');
          button.classList.add('success');
          
          // Auto-hide overlay after success
          if (CONFIG.autoHideAfterLock) {
            setTimeout(() => {
              hideOrientationWarning();
            }, 1500);
          }
        } else {
          throw new Error('Failed to lock orientation');
        }
      } else {
        throw new Error('Failed to enter fullscreen');
      }
    } catch (error) {
      // Show error state
      if (buttonText) buttonText.textContent = 'Unable to lock orientation';
      if (buttonIcon) buttonIcon.textContent = '❌';
      button.classList.remove('loading');
      button.classList.add('error');
      
      // Reset button after 2 seconds
      setTimeout(() => {
        button.classList.remove('error');
        button.disabled = false;
        if (buttonText) buttonText.textContent = 'Tap to Switch to Landscape';
        if (buttonIcon) buttonIcon.textContent = '🔓';
      }, 2000);
      
      if (CONFIG.debug) console.log('❌ Lock orientation failed:', error);
    }
  }

  // ═══════════════════════════════════════════════════════════════════
  // Main Orientation Check Logic
  // ═══════════════════════════════════════════════════════════════════
  
  function checkOrientation() {
    // Only enforce on mobile/tablet devices
    if (!isMobileOrTablet()) {
      hideOrientationWarning();
      return;
    }
    
    // Check if in portrait mode
    if (isPortraitMode()) {
      showOrientationWarning();
    } else {
      hideOrientationWarning();
    }
  }

  // ═══════════════════════════════════════════════════════════════════
  // Event Listeners
  // ═══════════════════════════════════════════════════════════════════
  
  function setupListeners() {
    // Listen for orientation changes
    window.addEventListener('orientationchange', () => {
      setTimeout(checkOrientation, 100); // Small delay for accurate measurements
    });
    
    // Listen for resize events (covers more cases than orientationchange)
    let resizeTimer;
    window.addEventListener('resize', () => {
      clearTimeout(resizeTimer);
      resizeTimer = setTimeout(checkOrientation, 100);
    });
    
    // Listen for screen orientation API changes (modern browsers)
    if (screen.orientation) {
      screen.orientation.addEventListener('change', () => {
        setTimeout(checkOrientation, 100);
      });
    }
  }

  // ═══════════════════════════════════════════════════════════════════
  // Initialization
  // ═══════════════════════════════════════════════════════════════════
  
  function init() {
    // Inject CSS styles
    injectStyles();
    
    // Setup event listeners
    setupListeners();
    
    // Perform initial check
    checkOrientation();
    
    if (CONFIG.debug) console.log('🎯 Landscape Enforcer initialized');
  }

  // ═══════════════════════════════════════════════════════════════════
  // CSS Injection
  // ═══════════════════════════════════════════════════════════════════
  
  function injectStyles() {
    const style = document.createElement('style');
    style.id = 'landscape-enforcer-styles';
    style.textContent = `
      #landscape-enforcer-overlay {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        z-index: 99999;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        opacity: 0;
        visibility: hidden;
        transition: opacity ${CONFIG.animationDuration}ms cubic-bezier(0.4, 0, 0.2, 1), 
                    visibility ${CONFIG.animationDuration}ms cubic-bezier(0.4, 0, 0.2, 1);
        backdrop-filter: blur(10px);
      }
      
      #landscape-enforcer-overlay.visible {
        opacity: 1;
        visibility: visible;
      }
      
      .landscape-enforcer-content {
        text-align: center;
        color: white;
        padding: 40px 30px;
        max-width: 90%;
        animation: fadeInUp 0.8s cubic-bezier(0.34, 1.56, 0.64, 1);
      }
      
      .landscape-enforcer-icon-wrapper {
        margin-bottom: 32px;
        display: flex;
        justify-content: center;
      }
      
      .landscape-enforcer-icon {
        color: white;
        opacity: 0.95;
        filter: drop-shadow(0 8px 16px rgba(0, 0, 0, 0.2));
      }
      
      .pulse-animation {
        animation: pulseGlow 2s ease-in-out infinite;
      }
      
      .rotate-animation {
        animation: rotateIcon 3s ease-in-out infinite;
      }
      
      .landscape-enforcer-title {
        font-size: 32px;
        font-weight: 800;
        margin: 0 0 16px 0;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
        letter-spacing: -0.5px;
      }
      
      .slide-in {
        animation: slideInTitle 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
      }
      
      .landscape-enforcer-message {
        font-size: 18px;
        margin: 0 0 32px 0;
        opacity: 0.95;
        line-height: 1.6;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        font-weight: 400;
      }
      
      .slide-in-delayed {
        animation: slideInMessage 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) 0.15s both;
      }
      
      /* Interactive Button Styles (Android) */
      .lock-orientation-button {
        position: relative;
        display: inline-flex;
        align-items: center;
        gap: 12px;
        padding: 18px 36px;
        font-size: 18px;
        font-weight: 700;
        color: #667eea;
        background: white;
        border: none;
        border-radius: 50px;
        cursor: pointer;
        overflow: hidden;
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        margin: 8px 0 20px 0;
        animation: buttonBounce 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) 0.3s both;
      }
      
      .lock-orientation-button:hover {
        transform: translateY(-2px) scale(1.02);
        box-shadow: 0 12px 32px rgba(0, 0, 0, 0.4);
      }
      
      .lock-orientation-button:active {
        transform: translateY(0) scale(0.98);
      }
      
      .lock-orientation-button.loading {
        pointer-events: none;
        opacity: 0.8;
      }
      
      .lock-orientation-button.loading .button-text {
        animation: pulse 1.5s ease-in-out infinite;
      }
      
      .lock-orientation-button.success {
        background: #10b981;
        color: white;
        animation: successPulse 0.6s ease-out;
      }
      
      .lock-orientation-button.error {
        background: #ef4444;
        color: white;
        animation: shake 0.5s ease-out;
      }
      
      .button-icon {
        font-size: 24px;
        transition: transform 0.3s ease;
      }
      
      .lock-orientation-button:hover .button-icon {
        transform: rotate(15deg) scale(1.1);
      }
      
      .button-text {
        font-weight: 600;
        letter-spacing: 0.3px;
      }
      
      .button-shine {
        position: absolute;
        top: 0;
        left: -100%;
        width: 100%;
        height: 100%;
        background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
        animation: shine 3s ease-in-out infinite;
      }
      
      .landscape-enforcer-hint {
        font-size: 14px;
        opacity: 0.8;
        margin: 0 0 24px 0;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 6px;
        animation: fadeIn 0.6s ease 0.45s both;
      }
      
      .hint-icon {
        animation: sparkle 2s ease-in-out infinite;
      }
      
      /* iOS Instruction Box */
      .ios-instruction-box {
        background: rgba(255, 255, 255, 0.15);
        border-radius: 16px;
        padding: 24px;
        margin: 24px 0;
        backdrop-filter: blur(10px);
        animation: fadeInUp 0.6s ease 0.3s both;
      }
      
      .instruction-step {
        display: flex;
        align-items: center;
        gap: 16px;
        margin: 16px 0;
        text-align: left;
      }
      
      .step-number {
        flex-shrink: 0;
        width: 36px;
        height: 36px;
        background: white;
        color: #667eea;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 700;
        font-size: 18px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
      }
      
      .step-text {
        flex: 1;
        font-size: 16px;
        line-height: 1.5;
      }
      
      /* Device Rotation Visual */
      .device-rotation-visual {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 30px;
        margin: 40px 0;
        animation: fadeInUp 0.8s ease 0.3s both;
      }
      
      /* Portrait Device (Left) */
      .portrait-device {
        width: 80px;
        height: 140px;
        background: white;
        border-radius: 12px;
        position: relative;
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
        animation: deviceBounce 2s ease-in-out infinite;
      }
      
      .portrait-device .device-screen {
        position: absolute;
        top: 8px;
        left: 8px;
        right: 8px;
        bottom: 20px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border-radius: 6px;
        overflow: hidden;
      }
      
      .portrait-device .device-button {
        position: absolute;
        bottom: 6px;
        left: 50%;
        transform: translateX(-50%);
        width: 30px;
        height: 4px;
        background: rgba(102, 126, 234, 0.3);
        border-radius: 2px;
      }
      
      /* Landscape Device (Right) */
      .landscape-device {
        width: 140px;
        height: 80px;
        background: white;
        border-radius: 12px;
        position: relative;
        box-shadow: 0 10px 40px rgba(255, 255, 255, 0.2);
        animation: deviceGlow 2s ease-in-out infinite;
      }
      
      .landscape-device .device-screen {
        position: absolute;
        top: 8px;
        left: 20px;
        right: 8px;
        bottom: 8px;
        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
        border-radius: 6px;
        overflow: hidden;
      }
      
      .landscape-device .device-button {
        position: absolute;
        left: 6px;
        top: 50%;
        transform: translateY(-50%);
        width: 4px;
        height: 30px;
        background: rgba(16, 185, 129, 0.3);
        border-radius: 2px;
      }
      
      /* Screen Content */
      .screen-content {
        padding: 8px;
        display: flex;
        flex-direction: column;
        gap: 4px;
      }
      
      .screen-line {
        height: 3px;
        background: rgba(255, 255, 255, 0.4);
        border-radius: 2px;
        width: 80%;
      }
      
      .screen-line.wide {
        width: 90%;
      }
      
      .screen-line:nth-child(2) {
        width: 60%;
      }
      
      .screen-line.wide:nth-child(2) {
        width: 70%;
      }
      
      /* Rotation Arrow */
      .rotation-arrow {
        flex-shrink: 0;
        animation: arrowPulse 1.5s ease-in-out infinite;
      }
      
      .rotation-arrow svg {
        filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.3));
      }
      
      /* Animations */
      @keyframes fadeInUp {
        from {
          opacity: 0;
          transform: translateY(30px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }
      
      @keyframes slideInTitle {
        from {
          opacity: 0;
          transform: translateX(-20px);
        }
        to {
          opacity: 1;
          transform: translateX(0);
        }
      }
      
      @keyframes slideInMessage {
        from {
          opacity: 0;
          transform: translateX(-20px);
        }
        to {
          opacity: 1;
          transform: translateX(0);
        }
      }
      
      @keyframes buttonBounce {
        from {
          opacity: 0;
          transform: scale(0.8);
        }
        to {
          opacity: 1;
          transform: scale(1);
        }
      }
      
      @keyframes pulseGlow {
        0%, 100% {
          opacity: 1;
          transform: scale(1);
          filter: drop-shadow(0 8px 16px rgba(0, 0, 0, 0.2));
        }
        50% {
          opacity: 0.8;
          transform: scale(1.05);
          filter: drop-shadow(0 12px 24px rgba(255, 255, 255, 0.3));
        }
      }
      
      @keyframes rotateIcon {
        0%, 100% {
          transform: rotate(0deg);
        }
        25% {
          transform: rotate(-10deg);
        }
        75% {
          transform: rotate(10deg);
        }
      }
      
      @keyframes pulse {
        0%, 100% {
          opacity: 1;
        }
        50% {
          opacity: 0.6;
        }
      }
      
      @keyframes shine {
        0% {
          left: -100%;
        }
        50%, 100% {
          left: 100%;
        }
      }
      
      @keyframes successPulse {
        0%, 100% {
          transform: scale(1);
        }
        50% {
          transform: scale(1.05);
        }
      }
      
      @keyframes shake {
        0%, 100% {
          transform: translateX(0);
        }
        25% {
          transform: translateX(-10px);
        }
        75% {
          transform: translateX(10px);
        }
      }
      
      @keyframes sparkle {
        0%, 100% {
          opacity: 0.8;
          transform: scale(1);
        }
        50% {
          opacity: 1;
          transform: scale(1.2);
        }
      }
      
      @keyframes fadeIn {
        from {
          opacity: 0;
        }
        to {
          opacity: 0.8;
        }
      }
      
      @keyframes deviceBounce {
        0%, 100% {
          transform: translateY(0);
        }
        50% {
          transform: translateY(-8px);
        }
      }
      
      @keyframes deviceGlow {
        0%, 100% {
          box-shadow: 0 10px 40px rgba(255, 255, 255, 0.2);
        }
        50% {
          box-shadow: 0 15px 50px rgba(16, 185, 129, 0.5);
        }
      }
      
      @keyframes arrowPulse {
        0%, 100% {
          opacity: 0.8;
          transform: scale(1);
        }
        50% {
          opacity: 1;
          transform: scale(1.1);
        }
      }
      
      /* Hide the overlay on desktop devices */
      @media (min-width: 1025px) {
        #landscape-enforcer-overlay {
          display: none !important;
        }
      }
      
      /* Additional enforcement - hide body scroll when overlay is visible */
      body:has(#landscape-enforcer-overlay.visible) {
        overflow: hidden;
      }
      
      /* Responsive adjustments */
      @media (max-width: 400px) {
        .landscape-enforcer-title {
          font-size: 26px;
        }
        
        .landscape-enforcer-message {
          font-size: 16px;
        }
        
        .lock-orientation-button {
          padding: 16px 28px;
          font-size: 16px;
        }
        
        .device-animation {
          font-size: 36px;
        }
      }
    `;
    
    document.head.appendChild(style);
  }

  // ═══════════════════════════════════════════════════════════════════
  // Auto-initialize when DOM is ready
  // ═══════════════════════════════════════════════════════════════════
  
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

  // Expose debug function to window (for testing)
  window.LandscapeEnforcer = {
    check: checkOrientation,
    show: showOrientationWarning,
    hide: hideOrientationWarning,
    isPortrait: isPortraitMode,
    isMobile: isMobileOrTablet,
    enableDebug: () => { CONFIG.debug = true; checkOrientation(); },
    disableDebug: () => { CONFIG.debug = false; }
  };

})();
