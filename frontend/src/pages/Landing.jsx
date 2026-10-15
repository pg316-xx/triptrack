import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Brand from '../components/Brand'
import Footer from '../components/Footer'
import {
  IconArrowRight, IconCalendar, IconCompass, IconFamily, IconHeart, IconLandmark, IconLeaf,
  IconMoney, IconMountain, IconPalm, IconPin, IconRoute, IconSearch, IconShield, IconSparkle,
  IconStar, IconTicket, IconUsers, Swash,
} from '../components/icons'

// Destination ideas. Durations are suggestions, not offers: TripNest plans trips, it doesn't sell them.
const DESTINATIONS = [
  { name: 'Swiss Alps', sub: 'Peaks & alpine villages', img: '/images/dest-alps.jpg', days: '6–8 days', tag: 'Adventure' },
  { name: 'Maldives', sub: 'Tropical paradise', img: '/images/dest-maldives.jpg', days: '5–7 days', tag: 'Beach' },
  { name: 'Kyoto, Japan', sub: 'Timeless culture', img: '/images/dest-kyoto.jpg', days: '4–6 days', tag: 'Culture' },
  { name: 'Iceland', sub: 'Land of fire & ice', img: '/images/dest-iceland.jpg', days: '7–10 days', tag: 'Nature' },
]

const CATEGORIES = [
  ['Adventure', IconMountain], ['Beach', IconPalm], ['Nature', IconLeaf],
  ['Culture', IconLandmark], ['Honeymoon', IconHeart], ['Family', IconFamily],
]

const FEATURES = [
  [IconRoute, 'Day-by-day itinerary', 'Drag to reorder; everyone sees it live.'],
  [IconMoney, 'Fair expense splits', 'Equal or custom, always to the cent.'],
  [IconShield, 'Private travel wallet', 'Tickets & IDs, shared only if you choose.'],
  [IconUsers, 'Plan with your crew', 'Invite links, roles and group chat.'],
]

// Stash what the visitor typed so the new-trip form is pre-filled after sign-up.
const saveDraft = (draft) => {
  try { sessionStorage.setItem('tripDraft', JSON.stringify(draft)) } catch { /* private mode */ }
}

export default function Landing() {
  const navigate = useNavigate()
  const [q, setQ] = useState({ destination: '', startDate: '', endDate: '', travelers: '2' })
  const set = (k) => (e) => setQ({ ...q, [k]: e.target.value })

  const start = (draft) => {
    saveDraft(draft)
    navigate('/register')
  }

  return (
    <>
      <nav className="nav">
        <div className="nav-inner">
          <Brand />
          <div className="links">
            <a href="#top" className="active">Home</a>
            <a href="#destinations">Destinations</a>
            <a href="#features">Features</a>
            <a href="#how">How it works</a>
          </div>
          <div className="tools">
            <Link to="/login" className="btn secondary small">Log in</Link>
            <Link to="/register" className="btn accent small">Sign up</Link>
          </div>
        </div>
      </nav>

      <main className="container" id="top" style={{ paddingTop: 18 }}>
        <section className="hero-photo" style={{ '--hero-img': 'url(/images/hero-lake.jpg)' }}>
          <IconLeaf className="leaf-deco" size={30} style={{ top: 16, left: 16, transform: 'rotate(-30deg)' }} />
          <div className="eyebrow">Plan together</div>
          <h1 style={{ marginTop: 14 }}>
            Discover Nature.<br />
            <span className="green">Plan Your Escape.<Swash /></span>
          </h1>
          <p className="lead">Itineraries, shared expenses and travel documents in one place, built for trips with friends.</p>
          <div className="actions">
            <a href="#plan" className="btn accent">Start planning <IconArrowRight size={18} /></a>
            <Link to="/login" className="btn secondary">I have an account</Link>
          </div>
        </section>

        <form id="plan" className="search-bar" onSubmit={(e) => { e.preventDefault(); start({ ...q, title: q.destination ? `Trip to ${q.destination}` : '' }) }}>
          <div className="seg">
            <IconPin />
            <div style={{ minWidth: 0, flex: 1 }}>
              <label htmlFor="where">Where to?</label>
              <input id="where" placeholder="Any destination" value={q.destination} onChange={set('destination')} />
            </div>
          </div>
          <div className="seg">
            <IconCalendar />
            <div style={{ minWidth: 0, flex: 1 }}>
              <label htmlFor="in">Start</label>
              <input id="in" type="date" value={q.startDate} onChange={set('startDate')} />
            </div>
          </div>
          <div className="seg">
            <IconCalendar />
            <div style={{ minWidth: 0, flex: 1 }}>
              <label htmlFor="out">End</label>
              <input id="out" type="date" min={q.startDate || undefined} value={q.endDate} onChange={set('endDate')} />
            </div>
          </div>
          <div className="seg">
            <IconUsers />
            <div style={{ minWidth: 0, flex: 1 }}>
              <label htmlFor="who">Travelers</label>
              <select id="who" value={q.travelers} onChange={set('travelers')}>
                {['1', '2', '3', '4', '5', '6+'].map((n) => <option key={n} value={n}>{n} traveler{n === '1' ? '' : 's'}</option>)}
              </select>
            </div>
          </div>
          <button>Plan trip <IconSearch size={16} /></button>
        </form>

        <section id="features" className="feature-strip">
          {FEATURES.map(([Icon, title, text]) => (
            <div key={title} className="feature">
              <div className="ic"><Icon size={20} /></div>
              <div><strong>{title}</strong><span>{text}</span></div>
            </div>
          ))}
        </section>

        <div id="destinations" className="section-title">
          <h2>Popular Destinations <IconLeaf className="leaf" size={18} /></h2>
          <Link to="/register" className="link-arrow">Plan one of these <span className="circle"><IconArrowRight size={14} /></span></Link>
        </div>
        <div className="dest-grid">
          {DESTINATIONS.map((d) => (
            <a key={d.name} href="#plan" className="dest-card" style={{ backgroundImage: `url(${d.img})` }}
              onClick={(e) => { e.preventDefault(); start({ title: `${d.name} trip`, destination: d.name }) }}>
              <div className="top"><span className="pill dark"><IconStar size={12} /> {d.tag}</span></div>
              <div className="bottom">
                <div><h3>{d.name}</h3><div className="sub">{d.sub}</div></div>
                <div className="price"><small>suggested</small>{d.days}</div>
              </div>
            </a>
          ))}
        </div>

        <div className="categories">
          {CATEGORIES.map(([name, Icon]) => (
            <button key={name} type="button" className="category" onClick={() => start({ title: `${name} trip` })}>
              <span className="ic"><Icon size={22} /></span>{name}
            </button>
          ))}
        </div>

        <section id="how" className="inspire">
          <div>
            <div className="eyebrow" style={{ color: 'var(--text)', letterSpacing: '0.04em', textTransform: 'none', fontSize: '0.85rem' }}>Need inspiration?</div>
            <h2>One trip, everyone on the same page</h2>
            <p>Plan the days together, log who paid for what, and settle up with the fewest payments.</p>
            <Link to="/register" className="btn">Create your first trip <IconArrowRight size={16} /></Link>
          </div>
          <div className="stories">
            <Link to="/register" className="story">
              <div className="img" style={{ backgroundImage: 'url(/images/story-trail.jpg)' }} />
              <div className="txt"><strong>Build the itinerary as a group</strong><span>Live updates · drag to reorder</span></div>
            </Link>
            <Link to="/register" className="story">
              <div className="img" style={{ backgroundImage: 'url(/images/story-backpacker.jpg)' }} />
              <div className="txt"><strong>Keep tickets and IDs handy</strong><span>Private wallet · share per trip</span></div>
            </Link>
          </div>
        </section>

        <section className="promo">
          <div className="img" style={{ backgroundImage: 'url(/images/banner-mountains.jpg)' }} />
          <div className="txt">
            <div className="eyebrow"><IconSparkle size={12} /> Free for groups</div>
            <h2>Your Next Adventure Starts Here</h2>
            <p>Invite friends with one link and plan everything together, from the first idea to the last split bill.</p>
            <Link to="/register" className="btn accent">Get started <IconArrowRight size={16} /></Link>
          </div>
        </section>

        <div className="stats-row">
          {[[IconRoute, 'Live', 'Itinerary sync'], [IconMoney, 'Exact', 'Expense splits'], [IconTicket, 'Private', 'Document wallet'], [IconCompass, '1 link', 'To invite your crew']].map(([Icon, big, label]) => (
            <div key={label} className="feature">
              <div className="ic"><Icon size={26} /></div>
              <div><div className="big">{big}</div><span>{label}</span></div>
            </div>
          ))}
        </div>
      </main>

      <Footer signedIn={false} />
    </>
  )
}
