// ── NP Core Logic ─────────────────────────────────────────────────────────
window.NP = (function() {
  let isOpen=false, all=[], activeFilter='all', expandedId=null, unsub=null, prevUnread=0;

  function resolveTs(raw) {
    if (!raw) return 0;
    if (typeof raw==='object' && typeof raw.seconds==='number')
      return raw.seconds*1000+Math.floor((raw.nanoseconds||0)/1e6);
    const n=Number(raw);
    if (!isFinite(n)||n<=0) return 0;
    return n<1e12?n*1000:n;
  }

  function emoji(t){return({listing_approved:'✅',listing_rejected:'❌',listing_pending:'⏳',listing_flagged:'🚩',property_unlocked:'🔓',account_suspended:'🚫',unlock_success:'🔑',payment_confirmed:'💳',property_updated:'✏️',property_removed:'🗑',welcome:'👋',system:'🔔'})[t]||'🔔';}
  function clr(t){if(['listing_approved','unlock_success','payment_confirmed'].includes(t))return'#10b981';if(['listing_rejected','property_removed','account_suspended'].includes(t))return'#ef4444';if(t==='listing_pending')return'#f59e0b';if(t==='listing_flagged')return'#f97316';if(t==='property_unlocked')return'#8b5cf6';if(t==='property_updated')return'#3b82f6';if(t==='welcome')return'#06b6d4';return'#94a3b8';}
  function lbl(t){return({listing_approved:'Approved',listing_rejected:'Rejected',listing_pending:'Pending',listing_flagged:'Flagged',property_unlocked:'Unlocked',account_suspended:'Suspended',unlock_success:'Unlock',payment_confirmed:'Payment',property_updated:'Updated',property_removed:'Removed',welcome:'Welcome',system:'System'})[t]||t||'Notice';}
  function fmtTime(ts){if(!ts)return'—';const d=Date.now()-ts;if(d<0)return'Just now';const m=Math.floor(d/60000),h=Math.floor(d/3600000),dy=Math.floor(d/86400000);if(m<1)return'Just now';if(m<60)return m+'m ago';if(h<24)return h+'h ago';if(dy===1)return'Yesterday';if(dy<7)return dy+'d ago';if(dy<30)return Math.floor(dy/7)+'w ago';return new Date(ts).toLocaleDateString('en-US',{month:'short',day:'numeric',year:'numeric'});}

  function recompute(){
    const unread=all.filter(n=>!n.isRead).length;
    const readCnt=all.filter(n=>n.isRead).length;
    const total=all.length;
    const today=all.filter(n=>new Date(n._ts).toDateString()===new Date().toDateString()).length;
    const badge=document.getElementById('notif-badge');
    if(badge){badge.textContent=unread>99?'99+':unread;badge.classList.toggle('hidden',unread===0);}
    const sv=(id,v)=>{const el=document.getElementById(id);if(el)el.textContent=v??'—';};
    sv('np-s-total',total);sv('np-s-unread',unread);sv('np-s-read',readCnt);sv('np-s-today',today);
    const sub=document.getElementById('np-subtitle');
    if(sub)sub.textContent=unread>0?`${unread} unread · ${readCnt} read`:'All caught up ✓';
    const ma=document.getElementById('np-mark-all');const ca=document.getElementById('np-clear-all');
    if(ma)ma.style.display=unread>0?'inline-block':'none';
    if(ca)ca.style.display=total>0?'inline-block':'none';
    if(prevUnread>0&&unread>prevUnread){const newest=all.find(n=>!n.isRead);pushNotif(newest);}
    prevUnread=unread;
    renderList();
  }

  function pushNotif(n){
    if(!n||!('Notification'in window)||Notification.permission!=='granted')return;
    try{new Notification('RentOut Admin — '+(n.title||'New notification'),{body:(n.message||'').substring(0,120),icon:'/android-chrome-192x192.png',tag:'ro-'+n.id,renotify:true});}catch(e){}
  }

  function getFiltered(){
    if(activeFilter==='unread')return all.filter(n=>!n.isRead);
    if(activeFilter==='read')return all.filter(n=>n.isRead);
    if(activeFilter==='payment')return all.filter(n=>['payment_confirmed','unlock_success','property_unlocked'].includes(n.type));
    if(activeFilter==='listing')return all.filter(n=>['listing_approved','listing_rejected','listing_pending','listing_flagged','property_updated','property_removed'].includes(n.type));
    return all;
  }

  function buildCard(n){
    const c=clr(n.type),u=!n.isRead,exp=expandedId===n.id;
    const bg=u?`linear-gradient(135deg,${c}08 0%,#fff 100%)`:'#fafafa';
    return `<div class="np-card${exp?' np-appear':''}" id="npc-${n.id}" style="background:${bg};border:1px solid ${u?c+'22':'#f0f0f0'};${u?`box-shadow:0 2px 10px ${c}14`:''}">`+
      `<div class="np-card-inner" onclick="NP.toggle('${n.id}')">`+
        `<div class="np-icon" style="background:${c}15">${emoji(n.type)}</div>`+
        `<div style="flex:1;min-width:0">`+
          `<div style="display:flex;align-items:center;gap:5px">`+
            `<span class="np-title" style="flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${n.title||'Notification'}</span>`+
            (u?`<div class="np-dot" style="background:${c}"></div>`:'')+
            `<span style="font-size:14px;color:#9ca3af;transition:transform .25s;display:inline-block;${exp?'transform:rotate(90deg)':''}">›</span>`+
          `</div>`+
          `<div class="np-preview">${n.message||''}</div>`+
          `<div class="np-meta">`+
            `<span class="np-tag" style="background:${c}15;color:${c}">${lbl(n.type)}</span>`+
            (n.role?`<span class="np-tag" style="background:#f1f5f9;color:#64748b">${n.role}</span>`:'')+
            `<span class="np-time">${fmtTime(n._ts)}</span>`+
          `</div>`+
        `</div>`+
      `</div>`+
      `<div class="np-expanded${exp?' open':''}" id="npe-${n.id}">`+
        `<div class="np-expanded-inner">`+
          `<div style="font-size:11px;color:#374151;line-height:1.6">${n.message||''}</div>`+
          (n.propertyTitle?`<div style="font-size:10px;color:#6b7280;margin-top:4px">🏠 ${n.propertyTitle}</div>`:'')+
          `<div style="display:flex;gap:6px;margin-top:8px;flex-wrap:wrap">`+
            (u?`<button class="np-action" style="background:#dbeafe;color:#1d4ed8" onclick="event.stopPropagation();NP.read('${n.id}')">✓ Mark read</button>`
              :`<button class="np-action" style="background:#f1f5f9;color:#64748b" onclick="event.stopPropagation();NP.unread('${n.id}')">↩ Mark unread</button>`)+
            `<button class="np-action" style="background:#fee2e2;color:#dc2626" onclick="event.stopPropagation();NP.del('${n.id}')">🗑 Delete</button>`+
          `</div>`+
        `</div>`+
      `</div>`+
    `</div>`;
  }

  function renderList(){
    const list=document.getElementById('np-list');if(!list)return;
    const items=getFiltered();
    const unreadItems=items.filter(n=>!n.isRead);
    const readItems=items.filter(n=>n.isRead);
    if(!items.length){
      const msgs={all:'Nothing here yet',unread:'You\'re all caught up! 🎉',read:'No read notifications',payment:'No payment notifications',listing:'No listing notifications'};
      list.innerHTML=`<div style="display:flex;flex-direction:column;align-items:center;justify-content:center;padding:48px 24px;gap:10px;text-align:center"><div style="font-size:36px">${activeFilter==='unread'?'🎉':'🔕'}</div><div style="font-size:13px;font-weight:700;color:#374151">${msgs[activeFilter]||'Nothing here'}</div><div style="font-size:11px;color:#94a3b8;line-height:1.6;max-width:200px">Notifications will appear here.</div></div>`;
      return;
    }
    let html='';
    if(activeFilter!=='read'&&unreadItems.length){
      html+=`<div class="np-section-hdr" style="background:rgba(255,245,245,.94)"><div class="np-section-label" style="color:#dc2626"><span style="width:8px;height:8px;border-radius:50%;background:#ef4444;display:inline-block;animation:pulseDot 2s infinite"></span>Unread</div><span class="np-section-count" style="background:#fee2e2;color:#dc2626">${unreadItems.length}</span></div>`;
      html+=unreadItems.map(buildCard).join('');
    }
    if(activeFilter!=='unread'&&readItems.length){
      html+=`<div class="np-section-hdr" style="background:rgba(240,253,244,.94);margin-top:${unreadItems.length?'8px':'0'}"><div class="np-section-label" style="color:#16a34a"><span style="font-size:12px">✅</span>Read</div><span class="np-section-count" style="background:#dcfce7;color:#16a34a">${readItems.length}</span></div>`;
      html+=readItems.map(buildCard).join('');
    }
    list.innerHTML=html;
  }

  function listen(){
    if(unsub)return;
    const {collection,query,orderBy,limit,onSnapshot}=window.__fsHelpers;
    const q=query(collection(window.db,'notifications'),orderBy('createdAt','desc'),limit(50));
    unsub=onSnapshot(q,{includeMetadataChanges:false},snap=>{
      all=snap.docs.map(d=>{const data=d.data();return{id:d.id,...data,_ts:resolveTs(data.createdAt)};});
      recompute();
    },err=>console.warn('NP:',err));
  }

  return {
    init(){
      if('Notification'in window&&Notification.permission==='default')Notification.requestPermission();
      if(!window.__fsHelpers || !window.db) {
        // Retry until the module script has set up db + helpers (handles timing variance across browsers)
        let retries = 0;
        const wait = setInterval(()=>{ retries++;
          if(window.__fsHelpers && window.db){ clearInterval(wait); listen(); }
          else if(retries > 20){ clearInterval(wait); console.warn('NP: db/__fsHelpers never became available after 2s'); }
        }, 100);
        return;
      }
      listen();
    },
    open(){isOpen=true;const p=document.getElementById('np-panel');const b=document.getElementById('np-backdrop');if(p)p.style.transform='translateX(0)';if(b)b.classList.remove('hidden');document.body.style.overflow='hidden';},
    close(){isOpen=false;const p=document.getElementById('np-panel');const b=document.getElementById('np-backdrop');if(p)p.style.transform='translateX(100%)';if(b)b.classList.add('hidden');document.body.style.overflow='';},
    toggle(notifId){
      const prev=expandedId;
      expandedId=expandedId===notifId?null:notifId;
      if(prev&&prev!==notifId){const pe=document.getElementById('npe-'+prev);if(pe)pe.classList.remove('open');}
      const ne=document.getElementById('npe-'+notifId);
      if(ne)ne.classList.toggle('open',expandedId===notifId);
    },
    filter(f){
      activeFilter=f;
      ['all','unread','read','payment','listing'].forEach(k=>{const el=document.getElementById('nptab-'+k);if(el)el.classList.toggle('active',k===f);});
      renderList();
    },
    async read(id){
      const idx=all.findIndex(n=>n.id===id);
      if(idx!==-1&&!all[idx].isRead){all[idx]={...all[idx],isRead:true};recompute();}
      const card=document.getElementById('npc-'+id);
      if(card){card.classList.add('np-reading');await new Promise(r=>setTimeout(r,420));}
      try{const{updateDoc,doc}=window.__fsHelpers;await updateDoc(doc(window.db,'notifications',id),{isRead:true});if(expandedId===id)expandedId=null;}
      catch(e){if(idx!==-1){all[idx]={...all[idx],isRead:false};recompute();}console.warn('NP.read:',e);}
    },
    async unread(id){
      const idx=all.findIndex(n=>n.id===id);
      if(idx!==-1){all[idx]={...all[idx],isRead:false};recompute();}
      try{const{updateDoc,doc}=window.__fsHelpers;await updateDoc(doc(window.db,'notifications',id),{isRead:false});if(expandedId===id)expandedId=null;}
      catch(e){if(idx!==-1){all[idx]={...all[idx],isRead:true};recompute();}console.warn('NP.unread:',e);}
    },
    async del(id){
      const card=document.getElementById('npc-'+id);
      if(card){card.classList.add('np-reading');await new Promise(r=>setTimeout(r,420));}
      try{const{deleteDoc,doc}=window.__fsHelpers;await deleteDoc(doc(window.db,'notifications',id));if(expandedId===id)expandedId=null;}
      catch(e){console.warn('NP.del:',e);}
    },
    async markAll(){
      const unread=all.filter(n=>!n.isRead);if(!unread.length)return;
      all=all.map(n=>({...n,isRead:true}));expandedId=null;recompute();
      const{writeBatch,doc}=window.__fsHelpers;const batch=writeBatch(window.db);
      unread.forEach(n=>batch.update(doc(window.db,'notifications',n.id),{isRead:true}));
      try{await batch.commit();}catch(e){all=all.map(n=>unread.find(u=>u.id===n.id)?{...n,isRead:false}:n);recompute();}
    },
    async clearAll(){
      if(!all.length)return;
      if(!confirm(`Delete all ${all.length} notifications? This cannot be undone.`))return;
      const{writeBatch,doc}=window.__fsHelpers;const batch=writeBatch(window.db);
      all.forEach(n=>batch.delete(doc(window.db,'notifications',n.id)));expandedId=null;
      try{await batch.commit();}catch(e){console.warn('NP.clearAll:',e);}
    }
  };
})();

document.addEventListener('keydown',e=>{if(e.key==='Escape')NP.close();});
window.toggleNotifPanel  = ()=>{const p=document.getElementById('np-panel');p&&p.style.transform==='translateX(0)'?NP.close():NP.open();};
window.markAllNotifsRead = ()=>NP.markAll();
window.markNotifRead     = id=>NP.read(id);

// Auto-init: poll until window.db and window.__fsHelpers are set by the page's module script.
// This ensures notifications load even if the page doesn't explicitly call NP.init().
(function autoInit(){
  let retries = 0;
  const timer = setInterval(()=>{
    retries++;
    if(window.db && window.__fsHelpers){ clearInterval(timer); NP.init(); }
    else if(retries > 50){ clearInterval(timer); } // give up after 5s
  }, 100);
})();
