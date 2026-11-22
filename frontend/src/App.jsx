import { useState, useEffect, useCallback } from 'react'

async function api(url, options) {
  const res = await fetch(url, options)
  let data = null
  try { data = await res.json() } catch (e) {}
  if (!res.ok) {
    const err = new Error((data && (data.error || data.message)) || 'HTTP ' + res.status)
    err.status = res.status
    throw err
  }
  return data
}

const JSON_HEADERS = { 'Content-Type': 'application/json' }

function Toast({ message }) {
  if (!message) return null
  return <div className="toast">{message}</div>
}

function Stats({ stats }) {
  return (
    <section className="stats">
      <div className="stat"><b>{stats.total}</b><span>Total</span></div>
      <div className="stat"><b>{stats.open}</b><span>Open</span></div>
      <div className="stat"><b>{stats.closed}</b><span>Closed</span></div>
    </section>
  )
}

function LoginPage({ onLoggedIn }) {
  const [mode, setMode] = useState('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit() {
    if (!username.trim() || !password) {
      setMessage('Enter a username and password')
      return
    }
    setBusy(true)
    setMessage('')
    try {
      if (mode === 'register') {
        await api('/auth/register', {
          method: 'POST',
          headers: JSON_HEADERS,
          body: JSON.stringify({ username: username.trim(), password })
        })
      }
      const res = await fetch('/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ username: username.trim(), password })
      })
      if (!res.ok) throw new Error('Wrong username or password')
      const me = await api('/auth/me')
      onLoggedIn(me)
    } catch (e) {
      setMessage(e.message)
    }
    setBusy(false)
  }

  return (
    <>
      <header>
        <h1>AI Helpdesk</h1>
        <p>Sign in to create and track your support tickets</p>
      </header>
      <div className="login-wrap">
        <section className="card">
          <h2>{mode === 'login' ? 'Login' : 'Create account'}</h2>
          <label htmlFor="u">Username</label>
          <input id="u" value={username} onChange={e => setUsername(e.target.value)} />
          <label htmlFor="p">Password</label>
          <input id="p" type="password" value={password}
                 onChange={e => setPassword(e.target.value)}
                 onKeyDown={e => { if (e.key === 'Enter') submit() }} />
          <div className="actions">
            <button className="btn-primary" disabled={busy} onClick={submit}>
              {mode === 'login' ? 'Login' : 'Register and login'}
            </button>
          </div>
          {message && <div className="ai err">{message}</div>}
          <p className="hint">
            {mode === 'login' ? 'New here? ' : 'Already have an account? '}
            <button className="link-btn"
                    onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setMessage('') }}>
              {mode === 'login' ? 'Create an account' : 'Login'}
            </button>
          </p>
        </section>
      </div>
    </>
  )
}

function TicketForm({ onCreate, notify }) {
  const [issue, setIssue] = useState('')
  const [priority, setPriority] = useState('MEDIUM')
  const [busy, setBusy] = useState(false)
  const [quick, setQuick] = useState(null)

  async function submit(withAi) {
    if (!issue.trim()) {
      notify('Please describe the issue')
      return
    }
    setBusy(true)
    try {
      await onCreate({ issueDescription: issue.trim(), priority }, withAi)
      setIssue('')
      setQuick(null)
    } catch (e) {
      notify('Could not create ticket: ' + e.message)
    }
    setBusy(false)
  }

  async function quickAnalyze() {
    if (!issue.trim()) { notify('Describe the issue first'); return }
    setBusy(true)
    setQuick({ text: 'AI is analyzing, please wait...' })
    try {
      const data = await api('/home/analyze', {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify({ issueDescription: issue.trim() })
      })
      setQuick({ text: data.aiSuggestion })
    } catch (e) {
      setQuick({ text: 'Error: ' + e.message, error: true })
    }
    setBusy(false)
  }

  return (
    <section className="card">
      <h2>Create ticket</h2>
      <div className="row">
        <div className="small">
          <label htmlFor="priority">Priority</label>
          <select id="priority" value={priority} onChange={e => setPriority(e.target.value)}>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
          </select>
        </div>
      </div>

      <label htmlFor="issue">Describe the issue</label>
      <textarea id="issue" rows="3" value={issue} onChange={e => setIssue(e.target.value)}
                placeholder="e.g. My laptop cannot connect to office Wi-Fi" />

      <div className="actions">
        <button disabled={busy} onClick={() => submit(false)}>Submit</button>
        <button className="btn-primary" disabled={busy} onClick={() => submit(true)}>
          Submit &amp; Analyze with AI
        </button>
        <button disabled={busy} onClick={quickAnalyze}>Analyze without saving</button>
      </div>

      {quick && <div className={'ai' + (quick.error ? ' err' : '')}>{quick.text}</div>}
    </section>
  )
}

function TicketCard({ ticket, onChanged, notify }) {
  const [ai, setAi] = useState(ticket.aiSuggestion || '')
  const [aiError, setAiError] = useState(false)
  const [loading, setLoading] = useState(false)

  async function analyze() {
    setLoading(true)
    setAiError(false)
    setAi('AI is analyzing, please wait...')
    try {
      const data = await api('/home/ticket/' + ticket.ticketId + '/analyze', { method: 'POST' })
      setAi(data.aiSuggestion)
    } catch (e) {
      setAi('Error: ' + e.message)
      setAiError(true)
    }
    setLoading(false)
  }

  async function close() {
    try {
      await api('/home/updateStatus/' + ticket.ticketId, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify({ status: 'CLOSED' })
      })
      notify('Ticket #' + ticket.ticketId + ' closed')
      onChanged()
    } catch (e) {
      notify('Could not close ticket: ' + e.message)
    }
  }

  async function remove() {
    if (!window.confirm('Delete ticket #' + ticket.ticketId + '?')) return
    try {
      await api('/home/ticket/' + ticket.ticketId, { method: 'DELETE' })
      notify('Ticket #' + ticket.ticketId + ' deleted')
      onChanged()
    } catch (e) {
      notify('Could not delete: ' + e.message)
    }
  }

  const priority = ticket.priority || 'MEDIUM'

  return (
    <div className="ticket">
      <div className="ticket-head">
        <span className="ticket-title">#{ticket.ticketId} {ticket.customerName}</span>
        <span>
          <span className={'badge b-' + priority}>{priority}</span>
          <span className={'badge b-' + ticket.status}>{ticket.status}</span>
        </span>
      </div>

      <p>{ticket.issueDescription}</p>

      <div className="actions" style={{ marginTop: 0 }}>
        <button className="btn-primary" disabled={loading} onClick={analyze}>
          {loading ? 'Analyzing...' : 'Analyze with AI'}
        </button>
        {ticket.status !== 'CLOSED' && <button onClick={close}>Close ticket</button>}
        <button className="btn-danger" onClick={remove}>Delete</button>
      </div>

      {ai && <div className={'ai' + (aiError ? ' err' : '')}>{ai}</div>}
    </div>
  )
}

function Dashboard({ user, onLogout }) {
  const [tickets, setTickets] = useState([])
  const [stats, setStats] = useState({ total: 0, open: 0, closed: 0 })
  const [filter, setFilter] = useState('')
  const [loadError, setLoadError] = useState('')
  const [toast, setToast] = useState('')

  const notify = useCallback((msg) => {
    setToast(msg)
    setTimeout(() => setToast(''), 4000)
  }, [])

  const load = useCallback(async () => {
    try {
      const list = await api('/home/tickets?status=' + encodeURIComponent(filter))
      list.sort((a, b) => b.ticketId - a.ticketId)
      setTickets(list)
      setStats(await api('/home/stats'))
      setLoadError('')
    } catch (e) {
      if (e.status === 401) { onLogout(false); return }
      setLoadError('Error loading tickets: ' + e.message)
    }
  }, [filter, onLogout])

  useEffect(() => { load() }, [load])

  async function createTicket(data, withAi) {
    const created = await api('/home/addTicket', {
      method: 'POST',
      headers: JSON_HEADERS,
      body: JSON.stringify(data)
    })
    notify('Ticket #' + created.ticketId + ' created')
    if (withAi) {
      try {
        await api('/home/ticket/' + created.ticketId + '/analyze', { method: 'POST' })
      } catch (e) {
        notify('Ticket saved, but AI failed: ' + e.message)
      }
    }
    await load()
  }

  return (
    <>
      <div className="topbar" style={{ background: '#4f46e5' }}>
        <span>{user.username}{user.admin ? ' (admin)' : ''}</span>
        <button onClick={() => onLogout(true)}>Logout</button>
      </div>
      <header style={{ paddingTop: 10 }}>
        <h1>AI Helpdesk</h1>
        <p>{user.admin ? 'Admin view: all tickets' : 'Your support tickets'}</p>
      </header>

      <main>
        <TicketForm onCreate={createTicket} notify={notify} />
        <Stats stats={stats} />

        <section className="card">
          <div className="toolbar">
            <h2>Tickets</h2>
            <select value={filter} onChange={e => setFilter(e.target.value)}>
              <option value="">All</option>
              <option value="OPEN">Open</option>
              <option value="CLOSED">Closed</option>
            </select>
          </div>

          {loadError && <div className="ai err">{loadError}</div>}
          {!loadError && tickets.length === 0 && <div className="empty">No tickets yet.</div>}
          {tickets.map(t => (
            <TicketCard key={t.ticketId + '-' + t.status + '-' + (t.aiSuggestion || '').length}
                        ticket={t} onChanged={load} notify={notify} />
          ))}
        </section>
      </main>

      <Toast message={toast} />
    </>
  )
}

function App() {
  const [user, setUser] = useState(null)
  const [checking, setChecking] = useState(true)

  useEffect(() => {
    api('/auth/me')
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setChecking(false))
  }, [])

  const logout = useCallback(async (callServer) => {
    if (callServer) {
      try { await fetch('/logout', { method: 'POST' }) } catch (e) {}
    }
    setUser(null)
  }, [])

  if (checking) return null
  if (!user) return <LoginPage onLoggedIn={setUser} />
  return <Dashboard user={user} onLogout={logout} />
}

export default App