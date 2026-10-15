import { IconLeaf } from './icons'

// Beige header panel used at the top of secondary pages (Wallet, Alerts, Join, Profile).
export default function PageBanner({ eyebrow, title, text, image, children }) {
  return (
    <section className="page-banner" style={image ? { paddingRight: 0, paddingTop: 0, paddingBottom: 0 } : undefined}>
      <div style={{ padding: image ? '30px 0 30px 0' : 0, flex: 1, minWidth: 240 }}>
        {eyebrow && <div className="eyebrow">{eyebrow}</div>}
        <h2>{title}</h2>
        {text && <p>{text}</p>}
        {children && <div className="row" style={{ marginTop: 16 }}>{children}</div>}
      </div>
      {image
        ? <div className="banner-img" style={{ backgroundImage: `url(${image})` }} />
        : <IconLeaf className="leaf-deco" size={64} />}
    </section>
  )
}
