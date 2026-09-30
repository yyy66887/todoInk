const { chromium }=require(process.env.TODOINK_PLAYWRIGHT || 'playwright');
const fs=require('fs');
const dir=__dirname.replaceAll('\\','/');
const url='file:///'+dir+'/todoink-preview.html';
const assert=(ok,m)=>{if(!ok)throw new Error(m)};
(async()=>{
 const b=await chromium.launch({headless:true,channel:'chrome'});const p=await b.newPage({viewport:{width:1440,height:1120}});const errors=[];p.on('pageerror',e=>errors.push(e.message));
 await p.goto(url);await p.screenshot({path:dir+'/preview-desktop.png',fullPage:true});
 await p.locator('[data-page="candidates"]').click();assert(await p.locator('#main-app .candidate').count()===3,'3 candidates');
 await p.locator('[data-action="review"][data-id="c2"]').click();await p.getByRole('button',{name:'确认加入待办',exact:true}).click();assert((await p.locator('#form-error').innerText()).includes('请选择截止方式'),'uncertain blocked');
 await p.getByLabel('截止精度').selectOption('none');assert(!(await p.getByLabel('截止日期').isVisible()),'no hidden exact date');
 await p.getByLabel('待办标题').fill('核对合同付款条款（已编辑）');await p.getByRole('button',{name:'确认加入待办',exact:true}).click();assert(await p.locator('#main-app .candidate').count()===2,'candidate confirmed once');
 await p.locator('#main-app [data-action="nav"][data-target="todo"]').click();await p.locator('#main-app [data-tab="all"]').click();assert((await p.locator('#main-app').innerText()).includes('核对合同付款条款（已编辑）'),'edited task present');
 await p.getByRole('button',{name:'完成：核对合同付款条款（已编辑）',exact:true}).click();await p.locator('#main-app [data-tab="completed"]').click();assert((await p.locator('#main-app').innerText()).includes('核对合同付款条款（已编辑）'),'completed task present');
 await p.locator('[data-page="candidates"]').click();await p.locator('#main-app [data-action="ignore"]').first().click();assert(await p.locator('#main-app .candidate').count()===1,'ignore removes candidate');
 await p.locator('[data-page="notifications"]').click();await p.locator('[data-filter="微信"]').click();assert(await p.locator('#main-app .notification').count()===2,'source filter');
 await p.locator('[data-page="sources"]').click();await p.getByLabel('搜索本机应用').fill('QQ');assert(await p.locator('[data-source]:visible').count()===1,'source search');
 await p.getByRole('switch',{name:'采集QQ通知'}).click();assert(await p.getByRole('switch',{name:'采集QQ通知'}).getAttribute('aria-checked')==='false','source toggle');
 await p.locator('[data-page="settings"]').click();await p.getByRole('switch',{name:'本地候选识别',exact:true}).click();assert(await p.getByRole('switch',{name:'本地候选识别',exact:true}).getAttribute('aria-checked')==='false','extraction toggle');
 await p.locator('#theme').click();await p.screenshot({path:dir+'/preview-dark.png',fullPage:true});
 await p.reload();await p.locator('[data-page="review"]').click();await p.getByLabel('待办标题').fill('   ');await p.getByRole('button',{name:'确认加入待办',exact:true}).click();assert((await p.locator('#form-error').innerText()).includes('请填写'),'empty title blocked');
 await p.reload();await p.locator('[data-page="review"]').click();await p.getByLabel('截止精度').selectOption('date');assert(!(await p.getByLabel('截止时刻').isVisible()),'date precision hides time');await p.screenshot({path:dir+'/preview-confirm.png',fullPage:true});
 const widths=[320,360,390,412];const layouts=[];for(const w of widths){await p.setViewportSize({width:w,height:1050});await p.goto(url);const measurements=await p.evaluate(()=>({doc:document.documentElement.scrollWidth,viewport:innerWidth,app:document.querySelector('.app').clientWidth,scroll:document.querySelector('.scroll').scrollWidth}));assert(measurements.doc<=w,'page overflow '+w);assert(measurements.scroll<=measurements.app,'app overflow '+w);layouts.push({width:w,...measurements});}
 await p.setViewportSize({width:320,height:1100});await p.screenshot({path:dir+'/preview-320.png',fullPage:true});
 await p.setViewportSize({width:1640,height:1180});await p.goto(url+'?overview=1');await p.screenshot({path:dir+'/page-overview.png',fullPage:true});assert(errors.length===0,'JS console errors');
 console.log(JSON.stringify({passed:['candidate confirmation','uncertain time validation','no-deadline choice','edited title preserved','completion archive','ignore','source filter','source search','source toggle','recognition toggle','empty title validation','date-only precision','light-dark navigation','responsive overflow'],layouts,errors},null,2));await b.close();
})().catch(e=>{console.error(e);process.exit(1)});

