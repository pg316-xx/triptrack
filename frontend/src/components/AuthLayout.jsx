import Brand from './Brand'

// Split auth screen: scenic photo panel + form card.
export default function AuthLayout({ title, text, children }) {
  return (
    <div className="auth-wrap">
      <aside className="auth-art" style={{ backgroundImage: 'url(/images/auth-boat.jpg)' }}>
        <Brand light />
        <div>
          <div className="eyebrow" style={{ color: '#f9c27d' }}>Explore the world together</div>
          <h2 style={{ marginTop: 10 }}>{title}</h2>
          <p>{text}</p>
        </div>
      </aside>
      <div className="auth-side">{children}</div>
    </div>
  )
}
