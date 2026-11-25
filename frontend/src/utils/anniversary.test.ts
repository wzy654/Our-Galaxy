import { describe, expect, it } from 'vitest'
import { getDaysTogether, getNextAnniversaryDays } from './anniversary'

describe('anniversary calendar helpers', () => {
  it('counts the anniversary date as the first day', () => {
    expect(getDaysTogether('2026-11-01', new Date(2026, 10, 25))).toBe(25)
  })

  it('returns a placeholder for missing or invalid dates', () => {
    expect(getDaysTogether(null)).toBeNull()
    expect(getNextAnniversaryDays('2026-02-30')).toBeNull()
  })

  it('calculates the next anniversary in the current or following year', () => {
    expect(getNextAnniversaryDays('2026-11-30', new Date(2026, 10, 25))).toBe(5)
    expect(getNextAnniversaryDays('2026-11-30', new Date(2026, 11, 1))).toBe(364)
  })

  it('clamps a leap-day anniversary to the last day of February', () => {
    expect(getNextAnniversaryDays('2024-02-29', new Date(2027, 1, 27))).toBe(1)
  })
})
