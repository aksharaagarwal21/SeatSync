import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { LogOut, Menu, X } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import { cn } from '../../lib/cn'
import { ButtonLink } from '../ui/Button'

export function Logo() {
  return (
    <Link to="/" className="flex items-center gap-2 rounded-md text-[15px] font-semibold tracking-tight text-zinc-900">
      <span className="flex size-7 items-center justify-center rounded-lg bg-brand-700" aria-hidden>
        <svg viewBox="0 0 32 32" className="size-4.5">
          <path d="M9 20v-6a3 3 0 0 1 3-3h8a3 3 0 0 1 3 3v6" fill="none" stroke="#fff" strokeWidth="2.6" strokeLinecap="round" />
          <path d="M7 20h18v3H7z" fill="#fff" />
        </svg>
      </span>
      SeatSync
    </Link>
  )
}

function navLinkClass({ isActive }: { isActive: boolean }) {
  return cn(
    'rounded-md px-3 py-2 text-sm font-medium transition-colors',
    isActive ? 'text-zinc-900' : 'text-zinc-500 hover:text-zinc-900',
  )
}

export function NavBar() {
  const { user, isAdmin, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    setMobileOpen(false)
    setMenuOpen(false)
  }, [location.pathname])

  useEffect(() => {
    if (!menuOpen) return
    const close = (event: MouseEvent | KeyboardEvent) => {
      if (event instanceof KeyboardEvent ? event.key === 'Escape' : !menuRef.current?.contains(event.target as Node)) {
        setMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', close)
    document.addEventListener('keydown', close)
    return () => {
      document.removeEventListener('mousedown', close)
      document.removeEventListener('keydown', close)
    }
  }, [menuOpen])

  const handleLogout = async () => {
    await logout()
    navigate('/')
  }

  const links = (
    <>
      <NavLink to="/" end className={navLinkClass}>
        Events
      </NavLink>
      {user && (
        <NavLink to="/bookings" className={navLinkClass}>
          My Bookings
        </NavLink>
      )}
      {isAdmin && (
        <NavLink to="/admin" className={navLinkClass}>
          Admin
        </NavLink>
      )}
    </>
  )

  const initials = user?.name
    .split(' ')
    .map((part) => part[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()

  return (
    <header className="sticky top-0 z-40 border-b border-zinc-200 bg-white/90 backdrop-blur supports-[backdrop-filter]:bg-white/80">
      <div className="mx-auto flex h-14 max-w-[1280px] items-center justify-between px-4 sm:px-6">
        <div className="flex items-center gap-6">
          <Logo />
          <nav className="hidden items-center gap-1 md:flex" aria-label="Main">
            {links}
          </nav>
        </div>

        <div className="flex items-center gap-2">
          {user ? (
            <div className="relative hidden md:block" ref={menuRef}>
              <button
                type="button"
                onClick={() => setMenuOpen((open) => !open)}
                aria-haspopup="menu"
                aria-expanded={menuOpen}
                aria-label={`Account menu for ${user.name}`}
                className="flex size-8 items-center justify-center rounded-full bg-zinc-900 text-xs font-semibold text-white hover:bg-zinc-700"
              >
                {initials}
              </button>
              {menuOpen && (
                <div role="menu" className="absolute right-0 mt-2 w-56 animate-fade-in rounded-xl border border-zinc-200 bg-white p-1.5 shadow-raised">
                  <div className="px-2.5 py-2">
                    <p className="truncate text-sm font-medium text-zinc-900">{user.name}</p>
                    <p className="truncate text-xs text-zinc-500">{user.email}</p>
                  </div>
                  <div className="my-1 h-px bg-zinc-100" />
                  <button
                    type="button"
                    role="menuitem"
                    onClick={handleLogout}
                    className="flex w-full items-center gap-2 rounded-lg px-2.5 py-2 text-sm text-zinc-700 hover:bg-zinc-50"
                  >
                    <LogOut className="size-4" aria-hidden /> Sign out
                  </button>
                </div>
              )}
            </div>
          ) : (
            <ButtonLink to="/login" state={{ from: location }} variant="secondary" size="sm" className="hidden md:inline-flex">
              Sign in
            </ButtonLink>
          )}
          <button
            type="button"
            className="-mr-2 rounded-md p-2 text-zinc-600 hover:text-zinc-900 md:hidden"
            onClick={() => setMobileOpen((open) => !open)}
            aria-expanded={mobileOpen}
            aria-controls="mobile-nav"
            aria-label={mobileOpen ? 'Close menu' : 'Open menu'}
          >
            {mobileOpen ? <X className="size-5" /> : <Menu className="size-5" />}
          </button>
        </div>
      </div>

      {mobileOpen && (
        <div id="mobile-nav" className="border-t border-zinc-200 bg-white px-4 py-3 md:hidden">
          <nav className="flex flex-col" aria-label="Main mobile">
            {links}
          </nav>
          <div className="mt-3 border-t border-zinc-100 pt-3">
            {user ? (
              <div className="flex items-center justify-between">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{user.name}</p>
                  <p className="truncate text-xs text-zinc-500">{user.email}</p>
                </div>
                <button type="button" onClick={handleLogout} className="rounded-md px-3 py-2 text-sm font-medium text-zinc-600">
                  Sign out
                </button>
              </div>
            ) : (
              <ButtonLink to="/login" state={{ from: location }} variant="secondary" className="w-full">
                Sign in
              </ButtonLink>
            )}
          </div>
        </div>
      )}
    </header>
  )
}
