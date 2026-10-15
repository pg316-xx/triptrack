// Thin line icons (24×24, currentColor) used across the UI.
const Svg = ({ children, size = 20, ...rest }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75"
    strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...rest}>
    {children}
  </svg>
)

export const IconSearch = (p) => <Svg {...p}><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></Svg>
export const IconPin = (p) => <Svg {...p}><path d="M12 21s-7-6.2-7-11.5A7 7 0 0 1 19 9.5C19 14.8 12 21 12 21Z" /><circle cx="12" cy="9.5" r="2.5" /></Svg>
export const IconCalendar = (p) => <Svg {...p}><rect x="3.5" y="5" width="17" height="15.5" rx="2.5" /><path d="M3.5 10h17M8 3v4M16 3v4" /></Svg>
export const IconUsers = (p) => <Svg {...p}><circle cx="9" cy="8" r="3.5" /><path d="M2.5 20a6.5 6.5 0 0 1 13 0" /><path d="M16 4.6a3.5 3.5 0 0 1 0 6.8M18.5 20a6.5 6.5 0 0 0-3-5.5" /></Svg>
export const IconArrowRight = (p) => <Svg {...p}><path d="M5 12h14M13 6l6 6-6 6" /></Svg>
export const IconArrowLeft = (p) => <Svg {...p}><path d="M19 12H5M11 6l-6 6 6 6" /></Svg>
export const IconBell = (p) => <Svg {...p}><path d="M6 9a6 6 0 1 1 12 0c0 6 2.5 7.5 2.5 7.5h-17S6 15 6 9Z" /><path d="M10 20a2 2 0 0 0 4 0" /></Svg>
export const IconWallet = (p) => <Svg {...p}><path d="M4 7.5A2.5 2.5 0 0 1 6.5 5H18v4" /><rect x="4" y="7.5" width="16.5" height="12" rx="2.5" /><circle cx="16" cy="13.5" r="1.2" fill="currentColor" stroke="none" /></Svg>
export const IconTicket = (p) => <Svg {...p}><path d="M3.5 8.5V6.5a1.5 1.5 0 0 1 1.5-1.5h14a1.5 1.5 0 0 1 1.5 1.5v2a2.5 2.5 0 0 0 0 5v2a1.5 1.5 0 0 1-1.5 1.5H5a1.5 1.5 0 0 1-1.5-1.5v-2a2.5 2.5 0 0 0 0-5Z" /><path d="M14 5v14" strokeDasharray="2 2.5" /></Svg>
export const IconCompass = (p) => <Svg {...p}><circle cx="12" cy="12" r="9" /><path d="m15.5 8.5-2 5-5 2 2-5 5-2Z" /></Svg>
export const IconTag = (p) => <Svg {...p}><path d="M3.5 12.5V4.5a1 1 0 0 1 1-1h8l8 8-9 9-8-8Z" /><circle cx="8.5" cy="8.5" r="1.5" /></Svg>
export const IconShield = (p) => <Svg {...p}><path d="M12 3 4.5 6v5.5c0 4.7 3.2 8.2 7.5 9.5 4.3-1.3 7.5-4.8 7.5-9.5V6L12 3Z" /><path d="m8.8 12 2.2 2.2 4.2-4.4" /></Svg>
export const IconHeadset = (p) => <Svg {...p}><path d="M4.5 13v-1a7.5 7.5 0 0 1 15 0v1" /><rect x="3.5" y="13" width="4" height="6" rx="1.5" /><rect x="16.5" y="13" width="4" height="6" rx="1.5" /><path d="M18.5 19c0 1.5-2 2.5-5 2.5" /></Svg>
export const IconMountain = (p) => <Svg {...p}><path d="m2.5 20 7-12 4 6.5 2.5-3.5 5.5 9h-19Z" /><path d="m7.6 11.2 2 1.3 1.6-1.6" /></Svg>
export const IconPalm = (p) => <Svg {...p}><path d="M12 21c0-5 .5-8 2-11" /><path d="M14 10c-1.5-3-5-3.5-8-2 2 0 4.5.5 6 3" /><path d="M14 10c1-3 4.5-4 7.5-2.5-2 0-4.5 1-5.5 3.5" /><path d="M14 10c2.5-1 5.5 1 6 4" /><path d="M5 21h14" /></Svg>
export const IconLeaf = (p) => <Svg {...p}><path d="M5 19c0-8 5-13.5 15-14-.5 10-6 15-14 15" /><path d="M5 19c3-4 6-6.5 9.5-8.5" /></Svg>
export const IconLandmark = (p) => <Svg {...p}><path d="M3.5 9 12 4l8.5 5h-17Z" /><path d="M5.5 9v8M9.8 9v8M14.2 9v8M18.5 9v8M3 20.5h18M4 17h16" /></Svg>
export const IconHeart = (p) => <Svg {...p}><path d="M12 20s-8-4.7-8-10.6A4.4 4.4 0 0 1 12 6.6a4.4 4.4 0 0 1 8 2.8C20 15.3 12 20 12 20Z" /></Svg>
export const IconFamily = (p) => <Svg {...p}><circle cx="7.5" cy="6.5" r="2.5" /><circle cx="16.5" cy="6.5" r="2.5" /><circle cx="12" cy="13" r="1.8" /><path d="M3.5 20v-4a4 4 0 0 1 8 0M12.5 16a4 4 0 0 1 8 0v4M9.5 20v-1.5a2.5 2.5 0 0 1 5 0V20" /></Svg>
export const IconStar = (p) => <Svg {...p}><path d="m12 3.5 2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.8l-5.2 2.8 1-5.8-4.3-4.1 5.9-.9L12 3.5Z" /></Svg>
export const IconClock = (p) => <Svg {...p}><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></Svg>
export const IconPlus = (p) => <Svg {...p}><path d="M12 5v14M5 12h14" /></Svg>
export const IconLogout = (p) => <Svg {...p}><path d="M14 4h4.5A1.5 1.5 0 0 1 20 5.5v13a1.5 1.5 0 0 1-1.5 1.5H14" /><path d="M10 16.5 5.5 12 10 7.5M5.5 12H15" /></Svg>
export const IconRoute = (p) => <Svg {...p}><circle cx="6" cy="18" r="2.5" /><circle cx="18" cy="6" r="2.5" /><path d="M8.5 18h7a3 3 0 0 0 0-6h-7a3 3 0 0 1 0-6h7" /></Svg>
export const IconMoney = (p) => <Svg {...p}><rect x="2.5" y="6" width="19" height="12" rx="2.5" /><circle cx="12" cy="12" r="2.8" /><path d="M6 9.5v5M18 9.5v5" /></Svg>
export const IconChat = (p) => <Svg {...p}><path d="M4 18.5V6.5A2.5 2.5 0 0 1 6.5 4h11A2.5 2.5 0 0 1 20 6.5v8a2.5 2.5 0 0 1-2.5 2.5H8.5L4 20.5" /></Svg>
export const IconFolder = (p) => <Svg {...p}><path d="M3.5 7.5A2.5 2.5 0 0 1 6 5h3.5l2 2.5H18a2.5 2.5 0 0 1 2.5 2.5v7.5A2.5 2.5 0 0 1 18 20H6a2.5 2.5 0 0 1-2.5-2.5v-10Z" /></Svg>
export const IconHistory = (p) => <Svg {...p}><path d="M4 12a8 8 0 1 0 2.3-5.6L4 8.7" /><path d="M4 4.5v4.2h4.2M12 8v4.5l3 1.8" /></Svg>
export const IconSparkle = (p) => <Svg {...p}><path d="M12 3c.7 4.2 2.8 6.3 7 7-4.2.7-6.3 2.8-7 7-.7-4.2-2.8-6.3-7-7 4.2-.7 6.3-2.8 7-7Z" /></Svg>

// Brand mark: orange sun rising behind a green mountain, in a circle.
export function Logo({ size = 40 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 48 48" aria-hidden="true">
      <circle cx="24" cy="24" r="23" fill="#fff4e6" />
      <circle cx="29" cy="19" r="8" fill="#f28a1e" />
      <path d="M4 37 17 18l8 11 5-6 14 14a23 23 0 0 1-40 0Z" fill="#1f4d36" />
      <path d="m17 18 3.2 4.4-2.2-1.2-2.4 2.3-1.4-.9L17 18Z" fill="#fff" opacity=".9" />
    </svg>
  )
}

// Orange brush-stroke underline used under a hero headline word.
export const Swash = () => (
  <svg className="swash" viewBox="0 0 220 18" preserveAspectRatio="none" aria-hidden="true">
    <path d="M3 13C50 5 120 3 217 9" fill="none" stroke="#f28a1e" strokeWidth="4.5" strokeLinecap="round" />
  </svg>
)
