// ── RentOut Admin — Shared Notification Panel ─────────────────────────────
// Include this file on every admin page. It expects:
//   - Firebase db already initialised as window.db
//   - Firestore helpers imported before this script runs

(function() {

// ── Auto-inject panel HTML + styles on script load ────────────────────────
injectPanelHTML();

function injectPanelHTML() {
  // Styles
  const style = document.createElement('style');
  style.textContent = `
    @keyframes notifPop   { 0%{opacity:0;transform:translateY(8px) scale(.97)} 100%{opacity:1;transform:none} }
    @keyframes pulseDot   { 0%,100%{transform:scale(1);opacity:1} 50%{transform:scale(1.5);opacity:.6} }
    @keyframes notifSlideRead { 0%{opacity:1;transform:translateX(0) scale(1)} 50%{opacity:.3;transform:translateX(40px) scale(.97)} 100%{opacity:0;transform:translateX(80px) scale(.95);max-height:0;margin:0;padding:0} }
    @keyframes notifAppear { 0%{opacity:0;transform:translateX(40px) scale(.95)} 100%{opacity:1;transform:none} }
    @keyframes shimmer { 0%{background-position:200% 0} 100%{background-position:-200% 0} }
    #np-panel { font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif; }
    .np-tab { font-size:11px;font-weight:600;padding:5px 11px;border-radius:20px;cursor:pointer;transition:all .15s;color:rgba(255,255,255,.6);background:transparent;border:none;white-space:nowrap; }
    .np-tab:hover { background:rgba(255,255,255,.12);color:#fff; }
    .np-tab.active { background:rgba(255,255,255,.22);color:#fff; }
    .np-section-hdr { display:flex;align-items:center;justify-content:space-between;padding:10px 14px 5px;position:sticky;top:0;z-index:2;backdrop-filter:blur(8px); }
    .np-section-label { font-size:10px;font-weight:800;letter-spacing:.08em;text-transform:uppercase;display:flex;align-items:center;gap:6px; }
    .np-section-count { font-size:10px;font-weight:700;padding:2px 8px;border-radius:20px;min-width:22px;text-align:center; }
    .np-card { margin:0 8px 6px;border-radius:14px;overflow:hidden;transition:box-shadow .2s,transform .15s;cursor:pointer; }
    .np-card:hover { box-shadow:0 4px 18px rgba(0,0,0,.09);transform:translateY(-1px); }
    .np-card-inner { display:flex;align-items:flex-start;gap:10px;padding:10px 12px 8px; }
    .np-icon { width:34px;height:34px;border-radius:10px;flex-shrink:0;display:flex;align-items:center;justify-content:center;font-size:15px; }
    .np-dot { width:7px;height:7px;border-radius:50%;flex-shrink:0;margin-top:4px;animation:pulseDot 2s ease-in-out infinite; }
    .np-title { font-size:11.5px;font-weight:700;color:#111827;line-height:1.3; }
    .np-preview { font-size:10.5px;color:#6b7280;line-height:1.45;margin-top:2px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden; }
    .np-expanded { max-height:0;overflow:hidden;transition:max-height .35s cubic-bezier(.4,0,.2,1),opacity .25s;opacity:0; }
    .np-expanded.open { max-height:240px;opacity:1; }
    .np-expanded-inner { padding:8px 12px 10px 56px;border-top:1px solid #f3f4f6; }
    .np-meta { display:flex;align-items:center;gap:5px;margin-top:5px;flex-wrap:wrap; }
    .np-tag { font-size:10px;font-weight:600;padding:2px 7px;border-radius:20px; }
    .np-time { font-size:10px;color:#9ca3af;margin-left:auto;white-space:nowrap; }
    .np-action { font-size:10px;font-weight:700;padding:4px 10px;border-radius:8px;border:none;cursor:pointer;transition:all .15s; }
    .np-reading { animation:notifSlideRead .42s cubic-bezier(.4,0,1,1) forwards;pointer-events:none; }
    .np-appear  { animation:notifAppear .36s cubic-bezier(0,0,.2,1.2) forwards; }
    .np-shimmer { height:64px;margin:0 8px 6px;border-radius:14px;background:linear-gradient(90deg,#f8fafc 25%,#f1f5f9 50%,#f8fafc 75%);background-size:200% 100%;animation:shimmer 1.3s infinite; }
    #np-list::-webkit-scrollbar { width:3px; }
    #np-list::-webkit-scrollbar-thumb { background:#e2e8f0;border-radius:4px; }
  `;
  document.head.appendChild(style);

  // Panel HTML
  const panel = document.createElement('div');
  panel.innerHTML = `
    <div id="np-panel" class="fixed top-0 right-0 bottom-0 z-50 flex flex-col bg-white"
      style="width:390px;max-width:100vw;box-shadow:-12px 0 48px rgba(0,0,0,.14);transform:translateX(100%);transition:transform .32s cubic-bezier(.4,0,.2,1);">
      <div style="background:linear-gradient(140deg,#0d1f6e 0%,#1B4FFF 55%,#4f7cff 100%);flex-shrink:0;">
        <div style="padding:16px 14px 0;">
          <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px;">
            <div style="display:flex;align-items:center;gap:9px;">
              <div style="width:34px;height:34px;background:rgba(255,255,255,.14);border-radius:11px;display:flex;align-items:center;justify-content:center;font-size:16px;">🔔</div>
              <div>
                <div style="color:#fff;font-size:15px;font-weight:800;letter-spacing:-.02em;">Notifications</div>
                <div id="np-subtitle" style="color:rgba(255,255,255,.6);font-size:10px;margin-top:1px;">Loading…</div>
              </div>
            </div>
            <div style="display:flex;align-items:center;gap:6px;">
              <button id="np-mark-all" onclick="NP.markAll()"
                style="display:none;font-size:10px;font-weight:700;color:#fff;background:rgba(255,255,255,.14);border:1px solid rgba(255,255,255,.22);border-radius:8px;padding:5px 11px;cursor:pointer;">
                ✓ Mark all read
              </button>
              <button id="np-clear-all" onclick="NP.clearAll()"
                style="display:none;font-size:10px;font-weight:700;color:rgba(255,255,255,.7);background:transparent;border:1px solid rgba(255,255,255,.18);border-radius:8px;padding:5px 10px;cursor:pointer;">
                🗑 Clear all
              </button>
              <button onclick="NP.close()"
                style="width:30px;height:30px;background:rgba(255,255,255,.13);border:none;border-radius:9px;color:#fff;font-size:20px;cursor:pointer;display:flex;align-items:center;justify-content:center;">
                &times;
              </button>
            </div>
          </div>
          <div style="display:flex;gap:8px;margin-bottom:10px;">
            <div style="flex:1;background:rgba(255,255,255,.1);border-radius:10px;padding:7px 6px;text-align:center;">
              <div id="np-s-total" style="color:#fff;font-size:17px;font-weight:800;">—</div>
              <div style="color:rgba(255,255,255,.5);font-size:9px;font-weight:600;letter-spacing:.06em;text-transform:uppercase;margin-top:2px;">Total</div>
            </div>
            <div style="flex:1;background:rgba(239,68,68,.25);border-radius:10px;padding:7px 6px;text-align:center;border:1px solid rgba(239,68,68,.4);">
              <div id="np-s-unread" style="color:#fca5a5;font-size:17px;font-weight:800;">—</div>
              <div style="color:rgba(252,165,165,.7);font-size:9px;font-weight:600;letter-spacing:.06em;text-transform:uppercase;margin-top:2px;">Unread</div>
            </div>
            <div style="flex:1;background:rgba(255,255,255,.1);border-radius:10px;padding:7px 6px;text-align:center;">
              <div id="np-s-read" style="color:#6ee7b7;font-size:17px;font-weight:800;">—</div>
              <div style="color:rgba(110,231,183,.6);font-size:9px;font-weight:600;letter-spacing:.06em;text-transform:uppercase;margin-top:2px;">Read</div>
            </div>
            <div style="flex:1;background:rgba(255,255,255,.1);border-radius:10px;padding:7px 6px;text-align:center;">
              <div id="np-s-today" style="color:#fde68a;font-size:17px;font-weight:800;">—</div>
              <div style="color:rgba(253,230,138,.6);font-size:9px;font-weight:600;letter-spacing:.06em;text-transform:uppercase;margin-top:2px;">Today</div>
            </div>
          </div>
          <div style="display:flex;gap:3px;overflow-x:auto;padding-bottom:1px;">
            <button class="np-tab active" id="nptab-all"     onclick="NP.filter('all')">All</button>
            <button class="np-tab"        id="nptab-unread"  onclick="NP.filter('unread')">🔴 Unread</button>
            <button class="np-tab"        id="nptab-read"    onclick="NP.filter('read')">✅ Read</button>
            <button class="np-tab"        id="nptab-payment" onclick="NP.filter('payment')">💳 Payments</button>
            <button class="np-tab"        id="nptab-listing" onclick="NP.filter('listing')">🏠 Listings</button>
          </div>
        </div>
        <div style="height:8px;background:linear-gradient(to bottom,transparent,rgba(0,0,0,.06));"></div>
      </div>
      <div id="np-list" style="flex:1;overflow-y:auto;padding:6px 0 20px;">
        <div class="np-shimmer"></div>
        <div class="np-shimmer" style="opacity:.7"></div>
        <div class="np-shimmer" style="opacity:.4"></div>
      </div>
    </div>
    <div id="np-backdrop" class="fixed inset-0 z-40 hidden"
      style="background:rgba(15,20,60,.28);backdrop-filter:blur(3px);"
      onclick="NP.close()"></div>
  `;
  document.body.appendChild(panel);
}
