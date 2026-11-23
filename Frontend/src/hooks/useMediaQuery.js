import { useEffect, useState } from 'react';

/** Breakpoints from styles/tokens.css, for use in `useMediaQuery`. */
export const BREAKPOINTS = { sm: 600, md: 900, lg: 1200, xl: 1600 };

/**
 * @param {string} query CSS media query, e.g. `(min-width: 900px)`.
 * @returns {boolean} Whether it currently matches.
 */
export function useMediaQuery(query) {
  const [matches, setMatches] = useState(() => window.matchMedia(query).matches);

  useEffect(() => {
    const list = window.matchMedia(query);
    const handleChange = () => setMatches(list.matches);
    handleChange();
    list.addEventListener('change', handleChange);
    return () => list.removeEventListener('change', handleChange);
  }, [query]);

  return matches;
}
