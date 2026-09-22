/**
 * DentalCare Management Portal — IT Team Command Center Module
 * Handles multi-agent orchestration, hashtag autocomplete, live chat feeds,
 * long-term memories, activity audit trails, and safe internal API runner.
 */

/**
 * Safe HTML escaping helper to prevent XSS in dynamic rendering
 */
function escapeHtml(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// IT Team State
let itTeamState = {
    currentSubTab: 'agents',
    agents: [],
    messages: [],
    memories: [],
    activitiesPage: { content: [], number: 0, totalPages: 1, totalElements: 0 },
    apiRuns: [],
    activeThreadParentId: null,
    selectedAgentFilter: '',
    selectedActionFilter: '',
    activityPage: 0
};

const IT_AGENTS_METADATA = {
    'it-backend': { icon: 'fa-server', color: 'text-sky-400', bg: 'bg-sky-500/20', border: 'border-sky-500/30' },
    'it-frontend': { icon: 'fa-laptop-code', color: 'text-purple-400', bg: 'bg-purple-500/20', border: 'border-purple-500/30' },
    'it-qa': { icon: 'fa-vial-circle-check', color: 'text-emerald-400', bg: 'bg-emerald-500/20', border: 'border-emerald-500/30' },
    'it-devops': { icon: 'fa-network-wired', color: 'text-amber-400', bg: 'bg-amber-500/20', border: 'border-amber-500/30' },
    'it-security': { icon: 'fa-shield-halved', color: 'text-rose-400', bg: 'bg-rose-500/20', border: 'border-rose-500/30' }
};

const IT_HASHTAGS = [
    { tag: '#it-backend', name: 'Alex Rivera (Backend Architect)', desc: 'Spring Boot, DB, APIs' },
    { tag: '#it-frontend', name: 'Maya Lin (Frontend Lead)', desc: 'Tailwind, Responsive UI' },
    { tag: '#it-qa', name: 'Samira Khan (QA Specialist)', desc: 'Regression, E2E Testing' },
    { tag: '#it-devops', name: 'Marcus Vance (DevOps Lead)', desc: 'CI/CD, Monitoring, Docker' },
    { tag: '#it-security', name: 'Chen Wei (Security Officer)', desc: 'RBAC, SSRF, Guardrails' }
];

/**
 * Initialize IT Team Command Center upon tab click
 */
function initItTeamCommandCenter() {
    switchItTeamSubView(itTeamState.currentSubTab || 'agents');
    setupHashtagAutocomplete();
}

/**
 * Switch between 5 sub-views in Command Center
 */
function switchItTeamSubView(subTabId) {
    itTeamState.currentSubTab = subTabId;
    const subViews = ['agents', 'chat', 'memories', 'activities', 'apimonitor'];

    subViews.forEach(v => {
        const sec = document.getElementById(`itteam-sub-${v}`);
        const btn = document.getElementById(`itteam-btn-${v}`);
        if (sec) sec.classList.toggle('hidden', v !== subTabId);
        if (btn) {
            if (v === subTabId) {
                btn.className = "px-4 py-2 rounded-xl bg-teal-600 text-white font-black text-xs shadow-md shadow-teal-600/25 flex items-center gap-2 transition";
            } else {
                btn.className = "px-4 py-2 rounded-xl bg-slate-800 text-slate-300 hover:text-white hover:bg-slate-700 font-bold text-xs flex items-center gap-2 transition";
            }
        }
    });

    if (subTabId === 'agents') loadItTeamAgents();
    else if (subTabId === 'chat') loadItTeamMessages();
    else if (subTabId === 'memories') loadItTeamMemories();
    else if (subTabId === 'activities') loadItTeamActivities(itTeamState.activityPage);
    else if (subTabId === 'apimonitor') loadItTeamApiRuns();
}

// =========================================================================
// 1. AGENT PROFILES & STATUS MANAGEMENT
// =========================================================================

async function loadItTeamAgents() {
    try {
        const res = await apiFetch('/api/it-team/agents');
        if (res.ok && res.data && res.data.data) {
            itTeamState.agents = res.data.data;
            renderAgentsGrid();
        }
    } catch (err) {
        console.error('Failed to load IT agents:', err);
    }
}

function renderAgentsGrid() {
    const grid = document.getElementById('itteam-agents-grid');
    if (!grid) return;

    if (!itTeamState.agents || itTeamState.agents.length === 0) {
        grid.innerHTML = `<div class="col-span-full text-center py-10 text-slate-400">Đang đồng bộ dữ liệu 5 đặc vụ IT...</div>`;
        return;
    }

    grid.innerHTML = itTeamState.agents.map(agent => {
        const code = agent.agentCode ? agent.agentCode.toLowerCase() : '';
        const meta = IT_AGENTS_METADATA[code] || IT_AGENTS_METADATA['it-' + code] || {
            icon: 'fa-robot', color: 'text-brand-400', bg: 'bg-brand-500/20', border: 'border-brand-500/30'
        };

        const statusBadgeColor = agent.status === 'ONLINE' ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30'
            : (agent.status === 'BUSY' ? 'bg-amber-500/20 text-amber-300 border-amber-500/30'
            : 'bg-slate-700/50 text-slate-400 border-slate-600');

        return `
            <div class="bg-slate-800/90 rounded-3xl p-6 border ${meta.border} shadow-xl flex flex-col justify-between space-y-4 hover:scale-[1.01] transition-transform">
                <div>
                    <div class="flex items-start justify-between gap-3 mb-3">
                        <div class="flex items-center gap-3">
                            <div class="w-12 h-12 rounded-2xl ${meta.bg} ${meta.color} flex items-center justify-center text-xl font-bold shadow-md">
                                <i class="fa-solid ${meta.icon}"></i>
                            </div>
                            <div>
                                <h4 class="text-sm font-black text-white">${escapeHtml(agent.displayName)}</h4>
                                <span class="font-mono text-xs font-bold ${meta.color}">${escapeHtml(agent.hashtag)}</span>
                            </div>
                        </div>
                        <span class="px-2.5 py-1 rounded-full text-[10px] font-black border ${statusBadgeColor}">
                            ${agent.status}
                        </span>
                    </div>

                    <div class="text-xs text-slate-300 font-semibold mb-2">
                        <i class="fa-solid fa-briefcase text-slate-400 mr-1"></i> ${escapeHtml(agent.role || '')}
                    </div>

                    <p class="text-xs text-slate-400 leading-relaxed line-clamp-2 mb-3">
                        ${escapeHtml(agent.expertise || '')}
                    </p>
                </div>

                <div class="pt-4 border-t border-slate-700/80 flex items-center justify-between gap-2">
                    <span class="text-[11px] text-slate-400 font-bold">Cập nhật trạng thái:</span>
                    <select onchange="handleAgentStatusChange(${agent.id}, this.value)"
                            class="bg-slate-900 border border-slate-700 text-slate-200 text-xs rounded-xl px-2.5 py-1.5 focus:ring-1 focus:ring-teal-500 cursor-pointer">
                        <option value="ONLINE" ${agent.status === 'ONLINE' ? 'selected' : ''}>🟢 ONLINE</option>
                        <option value="BUSY" ${agent.status === 'BUSY' ? 'selected' : ''}>🟡 BUSY</option>
                        <option value="OFFLINE" ${agent.status === 'OFFLINE' ? 'selected' : ''}>⚪ OFFLINE</option>
                    </select>
                </div>
            </div>
        `;
    }).join('');
}

async function handleAgentStatusChange(agentId, newStatus) {
    try {
        const res = await apiFetch(`/api/it-team/agents/${agentId}/status`, {
            method: 'PUT',
            body: JSON.stringify({ status: newStatus })
        });
        if (res.ok) {
            showToast(`✅ Đã cập nhật trạng thái đặc vụ sang ${newStatus}!`);
            loadItTeamAgents();
        } else {
            showToast('⚠️ Không thể cập nhật trạng thái đặc vụ.');
        }
    } catch (err) {
        showToast('⚠️ Lỗi kết nối khi cập nhật trạng thái.');
    }
}

// =========================================================================
// 2. INTER-AGENT MESSAGING & HASHTAG CONSOLE
// =========================================================================

async function loadItTeamMessages(parentMessageId = null) {
    try {
        let url = '/api/it-team/messages';
        if (parentMessageId) {
            url += `?parentMessageId=${parentMessageId}`;
        }
        const res = await apiFetch(url);
        if (res.ok && res.data && res.data.data) {
            if (parentMessageId) {
                renderThreadReplies(parentMessageId, res.data.data);
            } else {
                itTeamState.messages = res.data.data;
                renderMessagesFeed();
            }
        }
    } catch (err) {
        console.error('Failed to load IT messages:', err);
    }
}

function renderMessagesFeed() {
    const feed = document.getElementById('itteam-messages-feed');
    if (!feed) return;

    if (!itTeamState.messages || itTeamState.messages.length === 0) {
        feed.innerHTML = `
            <div class="text-center py-12 text-slate-400 space-y-2">
                <i class="fa-solid fa-comments text-3xl text-slate-600"></i>
                <div class="text-sm font-bold">Chưa có chỉ thị hoặc trao đổi nào</div>
                <p class="text-xs text-slate-500">Gửi tin nhắn với hashtag như #it-backend để bắt đầu điều phối.</p>
            </div>
        `;
        return;
    }

    feed.innerHTML = itTeamState.messages.map(msg => {
        const isAgent = msg.senderType === 'AGENT';
        const formattedTime = msg.sentAt ? new Date(msg.sentAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' }) : '';
        const bodyHtml = formatMessageBodyWithHashtags(msg.messageBody);

        return `
            <div class="p-4 rounded-2xl ${isAgent ? 'bg-slate-900/90 border border-slate-700/80' : 'bg-slate-800/80 border border-slate-700'} space-y-2">
                <div class="flex items-center justify-between gap-2">
                    <div class="flex items-center gap-2">
                        <span class="px-2 py-0.5 rounded-lg text-[10px] font-black ${isAgent ? 'bg-teal-500/20 text-teal-300' : 'bg-brand-500/20 text-brand-300'}">
                            ${escapeHtml(msg.senderType || 'USER')}
                        </span>
                        <span class="text-xs font-black text-white">${escapeHtml(msg.senderName || 'Anonymous')}</span>
                        ${msg.recipientHashtag ? `<span class="text-[11px] font-mono text-slate-400">→ ${escapeHtml(msg.recipientHashtag)}</span>` : ''}
                    </div>
                    <span class="text-[10px] text-slate-400">${formattedTime}</span>
                </div>

                <div class="text-xs text-slate-200 leading-relaxed font-mono whitespace-pre-wrap">${bodyHtml}</div>

                <div class="pt-2 flex items-center justify-between text-[11px] text-slate-400 border-t border-slate-700/40">
                    <button onclick="toggleThreadReplies(${msg.id})" class="hover:text-teal-400 flex items-center gap-1 font-bold cursor-pointer">
                        <i class="fa-solid fa-reply"></i> Luồng phản hồi & AI
                    </button>
                    <span class="text-[10px] text-slate-500">ID: #${msg.id}</span>
                </div>

                <div id="thread-replies-${msg.id}" class="hidden mt-3 pl-4 border-l-2 border-teal-500/40 space-y-2">
                    <div class="text-[11px] text-slate-400 italic">Đang tải phản hồi luồng...</div>
                </div>
            </div>
        `;
    }).join('');
}

function formatMessageBodyWithHashtags(body) {
    if (!body) return '';
    const escaped = escapeHtml(body);

    // Format triple-backtick markdown code blocks
    let formatted = escaped.replace(/```(?:[a-zA-Z0-9_-]+)?\r?\n?([\s\S]*?)```/g, (match, code) => {
        const trimmedCode = code ? code.trim() : '';
        return `<pre class="bg-slate-950 p-2.5 rounded-xl border border-slate-800 text-teal-300 my-1.5 overflow-x-auto font-mono text-[11px]">${trimmedCode}</pre>`;
    });

    return formatted.replace(/(#it-(?:backend|frontend|qa|devops|security))\b/gi, (match) => {
        return `<span class="bg-teal-500/20 text-teal-300 font-bold px-1.5 py-0.5 rounded border border-teal-500/30">${match}</span>`;
    });
}

function toggleThreadReplies(messageId) {
    const threadDiv = document.getElementById(`thread-replies-${messageId}`);
    if (!threadDiv) return;

    if (threadDiv.classList.contains('hidden')) {
        threadDiv.classList.remove('hidden');
        loadItTeamMessages(messageId);
    } else {
        threadDiv.classList.add('hidden');
    }
}

function renderThreadReplies(parentId, replies) {
    const threadDiv = document.getElementById(`thread-replies-${parentId}`);
    if (!threadDiv) return;

    if (!replies || replies.length === 0) {
        threadDiv.innerHTML = `
            <div class="text-xs text-slate-400 italic py-1">Chưa có phản hồi phụ trong luồng này.</div>
            <div class="mt-2 flex gap-2">
                <input type="text" id="reply-input-${parentId}" placeholder="Nhập phản hồi trong luồng..."
                       class="flex-1 bg-slate-950 border border-slate-700 rounded-xl px-3 py-1.5 text-xs text-white focus:ring-1 focus:ring-teal-500">
                <button onclick="sendThreadReply(${parentId})" class="px-3 py-1.5 bg-teal-600 hover:bg-teal-700 text-white text-xs font-bold rounded-xl transition cursor-pointer">
                    Gửi
                </button>
                <button onclick="askAiInThread(${parentId})" id="btn-ask-ai-thread-${parentId}" class="px-3 py-1.5 bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold rounded-xl transition flex items-center gap-1 cursor-pointer">
                    <i class="fa-solid fa-brain text-amber-300"></i> Hỏi AI
                </button>
            </div>
        `;
        return;
    }

    const repliesHtml = replies.map(r => {
        const time = r.sentAt ? new Date(r.sentAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '';
        return `
            <div class="bg-slate-950/80 p-3 rounded-xl border border-slate-800 text-xs space-y-1">
                <div class="flex items-center justify-between text-[10px]">
                    <span class="font-bold ${r.senderType === 'AGENT' ? 'text-teal-300' : 'text-slate-300'}">
                        ${r.senderType === 'AGENT' ? '🤖 ' : ''}${escapeHtml(r.senderName)}
                    </span>
                    <span class="text-slate-500">${time}</span>
                </div>
                <div class="text-slate-200 font-mono text-[11px] leading-relaxed">${formatMessageBodyWithHashtags(r.messageBody)}</div>
            </div>
        `;
    }).join('');

    threadDiv.innerHTML = `
        <div class="space-y-2 mb-2">${repliesHtml}</div>
        <div class="flex gap-2">
            <input type="text" id="reply-input-${parentId}" placeholder="Nhập câu hỏi tiếp theo trong luồng..."
                   class="flex-1 bg-slate-950 border border-slate-700 rounded-xl px-3 py-1.5 text-xs text-white focus:ring-1 focus:ring-teal-500">
            <button onclick="sendThreadReply(${parentId})" class="px-3 py-1.5 bg-teal-600 hover:bg-teal-700 text-white text-xs font-bold rounded-xl transition cursor-pointer">
                Gửi
            </button>
            <button onclick="askAiInThread(${parentId})" id="btn-ask-ai-thread-${parentId}" class="px-3 py-1.5 bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold rounded-xl transition flex items-center gap-1 cursor-pointer">
                <i class="fa-solid fa-brain text-amber-300"></i> Hỏi AI
            </button>
        </div>
    `;
}

async function askAiInThread(parentId) {
    const input = document.getElementById(`reply-input-${parentId}`);
    const btn = document.getElementById(`btn-ask-ai-thread-${parentId}`);
    if (!input || !input.value.trim()) {
        showToast('⚠️ Vui lòng nhập câu hỏi trong luồng cho AI.');
        return;
    }

    const question = input.value.trim();
    const origBtnHtml = btn ? btn.innerHTML : '';
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin mr-1"></i> AI suy luận...';
    }

    try {
        // Post user's question into thread first
        await apiFetch('/api/it-team/messages', {
            method: 'POST',
            body: JSON.stringify({
                messageBody: question,
                parentMessageId: parentId
            })
        });
        input.value = '';
        loadItTeamMessages(parentId);

        // Detect if specific agent is mentioned, or inherit from parent message
        const hashtagMatch = question.match(/(#it-(?:backend|frontend|qa|devops|security))\b/i);
        let endpoint = '/api/it-team/ask';
        let bodyPayload = { question: question };

        let matchedAgent = null;
        if (hashtagMatch && itTeamState.agents && itTeamState.agents.length > 0) {
            const targetTag = hashtagMatch[1].toLowerCase();
            matchedAgent = itTeamState.agents.find(a => (a.hashtag || '').toLowerCase() === targetTag);
        } else if (parentId && itTeamState.messages && itTeamState.messages.length > 0) {
            const parentMsg = itTeamState.messages.find(m => m.id === parentId);
            if (parentMsg && parentMsg.recipientHashtag && parentMsg.recipientHashtag !== 'BROADCAST') {
                const parentTag = parentMsg.recipientHashtag.toLowerCase();
                matchedAgent = itTeamState.agents.find(a => (a.hashtag || '').toLowerCase() === parentTag);
            }
        }

        if (matchedAgent && matchedAgent.id) {
            endpoint = `/api/it-team/ask/agent/${matchedAgent.id}`;
            bodyPayload = { message: question };
        }

        const res = await apiFetch(endpoint, {
            method: 'POST',
            body: JSON.stringify(bodyPayload)
        });

        if (res.ok && res.data && res.data.data) {
            const answer = res.data.data.answer || res.data.data.reply || '';
            const modelUsed = res.data.data.model || 'combo_toc_do';
            const agentTag = res.data.data.agent || (matchedAgent ? matchedAgent.hashtag : 'AI Copilot');

            // Post AI reply into thread
            await apiFetch('/api/it-team/messages', {
                method: 'POST',
                body: JSON.stringify({
                    messageBody: `🤖 **[${agentTag} | ${modelUsed}]**:\n\n${answer}`,
                    parentMessageId: parentId
                })
            });

            showToast('💡 Đã nhận phản hồi từ AI trong luồng!');
            loadItTeamMessages(parentId);
            loadItTeamActivities(0);
        } else {
            showToast('⚠️ 9Router không phản hồi kịp thời, vui lòng thử lại.');
            loadItTeamMessages(parentId);
        }
    } catch (err) {
        console.error('AI thread ask error:', err);
        showToast('⚠️ Lỗi kết nối 9Router AI trong luồng.');
        loadItTeamMessages(parentId);
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = origBtnHtml;
        }
    }
}

async function sendThreadReply(parentId) {
    const input = document.getElementById(`reply-input-${parentId}`);
    if (!input || !input.value.trim()) return;

    const messageBody = input.value.trim();
    try {
        const res = await apiFetch('/api/it-team/messages', {
            method: 'POST',
            body: JSON.stringify({
                messageBody: messageBody,
                parentMessageId: parentId
            })
        });
        if (res.ok) {
            input.value = '';
            showToast('✅ Đã gửi phản hồi luồng!');
            loadItTeamMessages(parentId);
        } else {
            showToast('⚠️ Lỗi gửi tin nhắn luồng.');
        }
    } catch (err) {
        showToast('⚠️ Lỗi kết nối gửi tin nhắn.');
    }
}

async function handleSendMessage(event) {
    if (event) event.preventDefault();
    const textarea = document.getElementById('itteam-message-input');
    if (!textarea || !textarea.value.trim()) {
        showToast('⚠️ Vui lòng nhập nội dung chỉ thị.');
        return;
    }

    const messageBody = textarea.value.trim();
    try {
        const res = await apiFetch('/api/it-team/messages', {
            method: 'POST',
            body: JSON.stringify({ messageBody })
        });
        if (res.ok) {
            textarea.value = '';
            showToast('🚀 Đã phát chỉ thị thành công!');
            loadItTeamMessages();
            // Also refresh activities if agent was mentioned
            loadItTeamActivities(0);
        } else {
            const errData = res.data && res.data.message ? res.data.message : 'Lỗi gửi tin nhắn';
            showToast(`⚠️ ${errData}`);
        }
    } catch (err) {
        showToast('⚠️ Lỗi kết nối gửi chỉ thị.');
    }
}

async function handleAskAiCopilot(event) {
    if (event) event.preventDefault();
    const textarea = document.getElementById('itteam-message-input');
    const btn = document.getElementById('btn-ask-ai-copilot');
    if (!textarea || !textarea.value.trim()) {
        showToast('⚠️ Vui lòng nhập câu hỏi hoặc yêu cầu tư vấn kỹ thuật.');
        return;
    }

    const question = textarea.value.trim();
    const origBtnHtml = btn ? btn.innerHTML : '';
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin mr-1"></i> Đang suy luận (combo_toc_do)...';
    }

    try {
        // Detect if specific agent hashtag is mentioned
        const hashtagMatch = question.match(/(#it-(?:backend|frontend|qa|devops|security))\b/i);
        let endpoint = '/api/it-team/ask';
        let bodyPayload = { question: question };

        if (hashtagMatch && itTeamState.agents && itTeamState.agents.length > 0) {
            const targetTag = hashtagMatch[1].toLowerCase();
            const matchedAgent = itTeamState.agents.find(a => (a.hashtag || '').toLowerCase() === targetTag);
            if (matchedAgent && matchedAgent.id) {
                endpoint = `/api/it-team/ask/agent/${matchedAgent.id}`;
                bodyPayload = { message: question };
            }
        }

        const res = await apiFetch(endpoint, {
            method: 'POST',
            body: JSON.stringify(bodyPayload)
        });

        if (res.ok && res.data && res.data.data) {
            const answer = res.data.data.answer || res.data.data.reply || '';
            const modelUsed = res.data.data.model || 'combo_toc_do';
            const agentTag = res.data.data.agent || 'AI Copilot';

            // Post response into the message stream
            await apiFetch('/api/it-team/messages', {
                method: 'POST',
                body: JSON.stringify({
                    messageBody: `🤖 **[${agentTag} | ${modelUsed}]**:\n\n${answer}`
                })
            });

            textarea.value = '';
            showToast('💡 Đã nhận tư vấn từ AI Copilot!');
            loadItTeamMessages();
            loadItTeamActivities(0);
        } else {
            showToast('⚠️ 9Router không phản hồi kịp thời, vui lòng thử lại.');
        }
    } catch (err) {
        console.error('AI ask error:', err);
        showToast('⚠️ Lỗi kết nối đến 9Router AI service.');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = origBtnHtml;
        }
    }
}

// =========================================================================
// HASHTAG AUTOCOMPLETE DROPDOWN
// =========================================================================

let isAutocompleteInitialized = false;
let autocompleteCandidates = [];
let activeCandidateIndex = -1;

function setupHashtagAutocomplete() {
    const textarea = document.getElementById('itteam-message-input');
    const dropdown = document.getElementById('itteam-hashtag-dropdown');
    if (!textarea || !dropdown) return;
    if (isAutocompleteInitialized) return;
    isAutocompleteInitialized = true;

    function renderDropdown() {
        if (!autocompleteCandidates || autocompleteCandidates.length === 0) {
            dropdown.classList.add('hidden');
            return;
        }

        dropdown.innerHTML = autocompleteCandidates.map((c, idx) => {
            const isActive = idx === activeCandidateIndex;
            const activeClasses = isActive
                ? 'bg-slate-700/90 ring-1 ring-teal-400/60'
                : 'hover:bg-slate-700/80';
            return `
                <div onclick="insertHashtag('${c.tag}')"
                     data-index="${idx}"
                     class="p-2.5 ${activeClasses} cursor-pointer rounded-xl flex items-center justify-between gap-3 transition">
                    <div>
                        <div class="font-mono text-xs font-black text-teal-300">${c.tag}</div>
                        <div class="text-[10px] text-slate-400">${escapeHtml(c.name)}</div>
                    </div>
                    <span class="text-[9px] text-slate-500 font-mono">${escapeHtml(c.desc)}</span>
                </div>
            `;
        }).join('');
        dropdown.classList.remove('hidden');
    }

    textarea.addEventListener('input', () => {
        const cursor = textarea.selectionStart;
        const textBeforeCursor = textarea.value.slice(0, cursor);
        const match = textBeforeCursor.match(/(?:^|\s)(#[\w-]*)$/);

        if (match) {
            const query = match[1].toLowerCase();
            autocompleteCandidates = IT_HASHTAGS.filter(h => h.tag.toLowerCase().startsWith(query));

            if (autocompleteCandidates.length > 0) {
                activeCandidateIndex = -1;
                renderDropdown();
                return;
            }
        }
        dropdown.classList.add('hidden');
        autocompleteCandidates = [];
        activeCandidateIndex = -1;
    });

    textarea.addEventListener('keydown', (e) => {
        if (dropdown.classList.contains('hidden') || autocompleteCandidates.length === 0) {
            return;
        }

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            activeCandidateIndex = (activeCandidateIndex + 1) % autocompleteCandidates.length;
            renderDropdown();
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            activeCandidateIndex = (activeCandidateIndex <= 0)
                ? autocompleteCandidates.length - 1
                : activeCandidateIndex - 1;
            renderDropdown();
        } else if (e.key === 'Enter' || e.key === 'Tab') {
            if (activeCandidateIndex >= 0 && activeCandidateIndex < autocompleteCandidates.length) {
                e.preventDefault();
                insertHashtag(autocompleteCandidates[activeCandidateIndex].tag);
            } else if (autocompleteCandidates.length === 1 && e.key === 'Tab') {
                e.preventDefault();
                insertHashtag(autocompleteCandidates[0].tag);
            }
        } else if (e.key === 'Escape') {
            e.preventDefault();
            dropdown.classList.add('hidden');
            autocompleteCandidates = [];
            activeCandidateIndex = -1;
        }
    });

    document.addEventListener('click', (e) => {
        if (!dropdown.contains(e.target) && e.target !== textarea) {
            dropdown.classList.add('hidden');
            autocompleteCandidates = [];
            activeCandidateIndex = -1;
        }
    });
}

function insertHashtag(tag) {
    const textarea = document.getElementById('itteam-message-input');
    const dropdown = document.getElementById('itteam-hashtag-dropdown');
    if (!textarea) return;

    const cursor = textarea.selectionStart;
    const textBeforeCursor = textarea.value.slice(0, cursor);
    const textAfterCursor = textarea.value.slice(cursor);

    const match = textBeforeCursor.match(/(?:^|\s)(#[\w-]*)$/);
    if (match) {
        const tagIndex = textBeforeCursor.length - match[1].length;
        const prefix = textBeforeCursor.slice(0, tagIndex);
        textarea.value = prefix + tag + ' ' + textAfterCursor;
        textarea.focus();
        const newCursor = (prefix + tag + ' ').length;
        textarea.setSelectionRange(newCursor, newCursor);
    } else {
        textarea.value += ' ' + tag + ' ';
        textarea.focus();
    }

    if (dropdown) dropdown.classList.add('hidden');
    autocompleteCandidates = [];
    activeCandidateIndex = -1;
}

// =========================================================================
// 3. PERSISTENT MEMORY STORE
// =========================================================================

async function loadItTeamMemories() {
    try {
        const agentCode = document.getElementById('memories-agent-filter') ? document.getElementById('memories-agent-filter').value : '';
        let url = '/api/it-team/memories';
        if (agentCode) {
            url += `?agentCode=${encodeURIComponent(agentCode)}`;
        }
        const res = await apiFetch(url);
        if (res.ok && res.data && res.data.data) {
            itTeamState.memories = res.data.data;
            renderMemoriesList();
        }
    } catch (err) {
        console.error('Failed to load memories:', err);
    }
}

function renderMemoriesList() {
    const list = document.getElementById('itteam-memories-list');
    if (!list) return;

    if (!itTeamState.memories || itTeamState.memories.length === 0) {
        list.innerHTML = `
            <div class="text-center py-10 text-slate-400">
                <i class="fa-solid fa-database text-2xl text-slate-600 mb-2"></i>
                <div class="text-xs">Chưa có khóa bộ nhớ nào được lưu.</div>
            </div>
        `;
        return;
    }

    list.innerHTML = itTeamState.memories.map(m => {
        const prioBadge = m.priority === 'HIGH' ? 'bg-rose-500/20 text-rose-300 border-rose-500/30'
            : (m.priority === 'LOW' ? 'bg-sky-500/20 text-sky-300 border-sky-500/30'
            : 'bg-amber-500/20 text-amber-300 border-amber-500/30');

        const updatedTime = m.lastUpdated ? new Date(m.lastUpdated).toLocaleDateString('vi-VN', {
            hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric'
        }) : '';

        return `
            <div class="bg-slate-800/90 rounded-2xl p-4 border border-slate-700 shadow-md space-y-2">
                <div class="flex items-center justify-between gap-2">
                    <div class="flex items-center gap-2">
                        <span class="font-mono text-xs font-black text-teal-400 bg-slate-900 px-2.5 py-1 rounded-lg border border-slate-700">
                            #${escapeHtml(m.agentCode || 'general')}
                        </span>
                        <span class="font-mono text-xs font-bold text-white tracking-wide">${escapeHtml(m.memoryKey)}</span>
                    </div>
                    <div class="flex items-center gap-2">
                        <span class="text-[10px] font-black px-2 py-0.5 rounded-full border ${prioBadge}">
                            ${escapeHtml(m.priority || 'MEDIUM')}
                        </span>
                        <button onclick="openEditMemoryModal('${escapeHtml(m.agentCode)}', '${escapeHtml(m.memoryKey)}', '${escapeHtml(m.priority)}', this)"
                                class="text-xs text-teal-400 hover:text-teal-300 font-bold px-2 py-0.5 bg-slate-900/60 rounded-lg hover:bg-slate-700/60 border border-slate-700/50 transition flex items-center gap-1 cursor-pointer">
                            <i class="fa-solid fa-pen-to-square"></i> Sửa
                        </button>
                    </div>
                </div>

                <div class="bg-slate-900/80 p-3 rounded-xl border border-slate-800 text-xs text-slate-200 font-mono whitespace-pre-wrap leading-relaxed">
                    ${escapeHtml(m.memoryContent || '')}
                </div>

                <div class="text-[10px] text-slate-400 text-right">
                    Cập nhật lần cuối: ${updatedTime}
                </div>
            </div>
        `;
    }).join('');
}

function openCreateMemoryModal() {
    const modal = document.getElementById('itteam-memory-modal');
    if (modal) modal.classList.remove('hidden');
}

function openEditMemoryModal(agentCode, memoryKey, priority, el) {
    const agentSelect = document.getElementById('mem-modal-agent');
    const keyInput = document.getElementById('mem-modal-key');
    const contentInput = document.getElementById('mem-modal-content');
    const prioritySelect = document.getElementById('mem-modal-priority');

    let normalizedAgent = agentCode || 'it-backend';
    if (normalizedAgent && !normalizedAgent.startsWith('it-')) {
        normalizedAgent = 'it-' + normalizedAgent;
    }

    if (agentSelect) agentSelect.value = normalizedAgent;
    if (keyInput) keyInput.value = memoryKey || '';
    if (prioritySelect) prioritySelect.value = priority || 'MEDIUM';

    // Extract memory content from state or DOM
    let content = '';
    if (itTeamState.memories && itTeamState.memories.length > 0) {
        const found = itTeamState.memories.find(m =>
            (m.agentCode === agentCode || m.agentCode === normalizedAgent) && m.memoryKey === memoryKey
        );
        if (found) content = found.memoryContent || '';
    }

    if (!content && el) {
        const card = el.closest('.bg-slate-800\\/90') || el.closest('.space-y-2');
        if (card) {
            const contentEl = card.querySelector('.whitespace-pre-wrap');
            if (contentEl) content = contentEl.textContent.trim();
        }
    }

    if (contentInput) contentInput.value = content;

    openCreateMemoryModal();
}

function closeCreateMemoryModal() {
    const modal = document.getElementById('itteam-memory-modal');
    if (modal) modal.classList.add('hidden');
}

async function handleSaveMemory(event) {
    if (event) event.preventDefault();

    const agentCode = document.getElementById('mem-modal-agent').value;
    const memoryKey = document.getElementById('mem-modal-key').value.trim();
    const memoryContent = document.getElementById('mem-modal-content').value.trim();
    const priority = document.getElementById('mem-modal-priority').value;

    if (!memoryKey || !memoryContent) {
        showToast('⚠️ Khóa và nội dung bộ nhớ không được để trống.');
        return;
    }

    try {
        const res = await apiFetch('/api/it-team/memories', {
            method: 'POST',
            body: JSON.stringify({ agentCode, memoryKey, memoryContent, priority })
        });
        if (res.ok) {
            showToast('✅ Đã lưu bộ nhớ thành công!');
            closeCreateMemoryModal();
            loadItTeamMemories();
        } else {
            showToast('⚠️ Không thể lưu bộ nhớ.');
        }
    } catch (err) {
        showToast('⚠️ Lỗi kết nối lưu bộ nhớ.');
    }
}

// =========================================================================
// 4. ACTIVITY AUDIT TRAIL
// =========================================================================

async function loadItTeamActivities(page = 0) {
    itTeamState.activityPage = Math.max(0, page);
    try {
        const agentCode = document.getElementById('activities-agent-filter') ? document.getElementById('activities-agent-filter').value : '';
        const actionType = document.getElementById('activities-action-filter') ? document.getElementById('activities-action-filter').value : '';

        let url = `/api/it-team/activities?page=${itTeamState.activityPage}&size=10`;
        if (agentCode) url += `&agentCode=${encodeURIComponent(agentCode)}`;
        if (actionType) url += `&actionType=${encodeURIComponent(actionType)}`;

        const res = await apiFetch(url);
        if (res.ok && res.data && res.data.data) {
            itTeamState.activitiesPage = res.data.data;
            renderActivitiesTable();
        }
    } catch (err) {
        console.error('Failed to load activities:', err);
    }
}

function renderActivitiesTable() {
    const tbody = document.getElementById('itteam-activities-table-body');
    const pageInfo = document.getElementById('itteam-activities-page-info');
    if (!tbody) return;

    const items = itTeamState.activitiesPage.content || [];
    if (items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center py-8 text-slate-400">Không có nhật ký hoạt động nào phù hợp.</td></tr>`;
        if (pageInfo) pageInfo.innerText = 'Trang 1 / 1 (0 bản ghi)';
        return;
    }

    tbody.innerHTML = items.map(act => {
        const time = act.timestamp ? new Date(act.timestamp).toLocaleTimeString('vi-VN', {
            hour: '2-digit', minute: '2-digit', second: '2-digit', day: '2-digit', month: '2-digit'
        }) : '';

        const actionBadgeColor = act.actionType === 'MENTIONED' ? 'bg-purple-500/20 text-purple-300 border-purple-500/30'
            : (act.actionType === 'API_RUN' ? 'bg-sky-500/20 text-sky-300 border-sky-500/30'
            : (act.actionType === 'STATUS_UPDATE' ? 'bg-amber-500/20 text-amber-300 border-amber-500/30'
            : 'bg-teal-500/20 text-teal-300 border-teal-500/30'));

        return `
            <tr class="hover:bg-slate-850 transition">
                <td class="py-3 px-4 font-mono text-[11px] text-slate-400 whitespace-nowrap">${time}</td>
                <td class="py-3 px-4">
                    <span class="font-mono text-xs font-bold text-teal-300 bg-slate-900 px-2 py-0.5 rounded border border-slate-700">
                        ${escapeHtml(act.agentCode || '-')}
                    </span>
                </td>
                <td class="py-3 px-4">
                    <span class="px-2 py-0.5 rounded-full text-[10px] font-black border ${actionBadgeColor}">
                        ${escapeHtml(act.actionType || '')}
                    </span>
                </td>
                <td class="py-3 px-4 text-xs text-slate-200 font-mono">${escapeHtml(act.description || '')}</td>
                <td class="py-3 px-4 font-mono text-[11px] text-slate-400 text-right whitespace-nowrap">
                    ${escapeHtml(act.relatedEntityLink || '-')}
                </td>
            </tr>
        `;
    }).join('');

    if (pageInfo) {
        const current = (itTeamState.activitiesPage.number || 0) + 1;
        const total = itTeamState.activitiesPage.totalPages || 1;
        const count = itTeamState.activitiesPage.totalElements || items.length;
        pageInfo.innerText = `Trang ${current} / ${total} (${count} sự kiện)`;
    }
}

function prevActivityPage() {
    if (itTeamState.activityPage > 0) {
        loadItTeamActivities(itTeamState.activityPage - 1);
    }
}

function nextActivityPage() {
    const total = itTeamState.activitiesPage.totalPages || 1;
    if (itTeamState.activityPage + 1 < total) {
        loadItTeamActivities(itTeamState.activityPage + 1);
    }
}

// =========================================================================
// 5. SAFE LOCALHOST API RUNNER & MONITOR
// =========================================================================

async function handleExecuteApiRun(event) {
    if (event) event.preventDefault();

    const method = document.getElementById('apirun-method').value;
    const endpoint = document.getElementById('apirun-endpoint').value.trim();
    const payload = document.getElementById('apirun-payload').value.trim();

    if (!endpoint) {
        showToast('⚠️ Vui lòng nhập đường dẫn API endpoint.');
        return;
    }

    const resultBox = document.getElementById('apirun-result-container');
    if (resultBox) resultBox.classList.remove('hidden');

    const statusBadge = document.getElementById('apirun-res-status');
    const durationBadge = document.getElementById('apirun-res-duration');
    const bodyBox = document.getElementById('apirun-res-body');

    if (statusBadge) statusBadge.innerText = 'Đang gọi...';
    if (durationBadge) durationBadge.innerText = '-- ms';
    if (bodyBox) bodyBox.innerText = 'Đang thực thi an toàn qua MockMvc pipeline...';

    try {
        const res = await apiFetch('/api/it-team/api-runs', {
            method: 'POST',
            body: JSON.stringify({
                httpMethod: method,
                endpoint: endpoint,
                requestPayload: payload || null
            })
        });

        if (res.ok && res.data && res.data.data) {
            const run = res.data.data;
            if (statusBadge) {
                statusBadge.innerText = `${run.statusCode || '200'} ${run.statusCode >= 200 && run.statusCode < 300 ? 'OK' : 'FAIL'}`;
                statusBadge.className = `px-2.5 py-1 rounded-full text-xs font-black border ${run.statusCode >= 200 && run.statusCode < 300 ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30' : 'bg-rose-500/20 text-rose-300 border-rose-500/30'}`;
            }
            if (durationBadge) durationBadge.innerText = `${run.executionDurationMs} ms`;
            if (bodyBox) {
                try {
                    const parsed = JSON.parse(run.responsePayload);
                    bodyBox.innerText = JSON.stringify(parsed, null, 2);
                } catch (e) {
                    bodyBox.innerText = run.responsePayload || 'Không có phản hồi rỗng';
                }
            }
            showToast('✅ Đã thực thi gọi API thành công!');
            loadItTeamApiRuns();
        } else {
            const errMsg = res.data && res.data.message ? res.data.message : 'Lỗi thực thi API';
            if (statusBadge) {
                statusBadge.innerText = `HTTP ${res.status || '400'} ERROR`;
                statusBadge.className = "px-2.5 py-1 rounded-full text-xs font-black border bg-rose-500/20 text-rose-300 border-rose-500/30";
            }
            if (bodyBox) bodyBox.innerText = `Lỗi: ${errMsg}`;
            showToast(`⚠️ ${errMsg}`);
        }
    } catch (err) {
        if (statusBadge) statusBadge.innerText = 'EXCEPTION';
        if (bodyBox) bodyBox.innerText = `Network or SSRF error: ${err.message}`;
        showToast('⚠️ Lỗi gọi kiểm thử API.');
    }
}

async function loadItTeamApiRuns() {
    try {
        const res = await apiFetch('/api/it-team/api-runs');
        if (res.ok && res.data && res.data.data) {
            itTeamState.apiRuns = res.data.data;
            renderApiRunsTable();
        }
    } catch (err) {
        console.error('Failed to load API runs:', err);
    }
}

function renderApiRunsTable() {
    const tbody = document.getElementById('apirun-history-table-body');
    if (!tbody) return;

    if (!itTeamState.apiRuns || itTeamState.apiRuns.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-6 text-slate-400">Chưa có lịch sử chạy kiểm thử API nào.</td></tr>`;
        return;
    }

    tbody.innerHTML = itTeamState.apiRuns.slice(0, 15).map(run => {
        const time = run.runTimestamp ? new Date(run.runTimestamp).toLocaleTimeString('vi-VN', {
            hour: '2-digit', minute: '2-digit', second: '2-digit'
        }) : '';

        const isSuccess = run.statusCode >= 200 && run.statusCode < 400;
        const statusClass = isSuccess ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30'
            : (run.statusCode === 404 ? 'bg-amber-500/20 text-amber-300 border-amber-500/30' : 'bg-rose-500/20 text-rose-300 border-rose-500/30');

        return `
            <tr class="hover:bg-slate-850 transition text-xs">
                <td class="py-3 px-4 font-mono text-[11px] text-slate-400 whitespace-nowrap">${time}</td>
                <td class="py-3 px-4">
                    <span class="font-bold text-teal-300 bg-slate-900 px-2 py-0.5 rounded border border-slate-700">${run.httpMethod}</span>
                </td>
                <td class="py-3 px-4 font-mono text-slate-200">${escapeHtml(run.endpoint)}</td>
                <td class="py-3 px-4">
                    <span class="px-2 py-0.5 rounded-full text-[10px] font-black border ${statusClass}">
                        ${run.statusCode || 'ERR'}
                    </span>
                </td>
                <td class="py-3 px-4 font-mono text-slate-300">${run.executionDurationMs} ms</td>
                <td class="py-3 px-4 text-slate-400 text-right">${escapeHtml(run.initiatedBy || 'admin')}</td>
            </tr>
        `;
    }).join('');
}

function setApiPreset(method, endpoint, payload = '') {
    const methodInput = document.getElementById('apirun-method');
    const endpointInput = document.getElementById('apirun-endpoint');
    const payloadInput = document.getElementById('apirun-payload');

    if (methodInput) methodInput.value = method;
    if (endpointInput) endpointInput.value = endpoint;
    if (payloadInput) payloadInput.value = payload;
}
