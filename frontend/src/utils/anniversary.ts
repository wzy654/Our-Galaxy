interface CalendarDate {
  year: number
  month: number
  day: number
}

function parseDate(value: string | null): CalendarDate | null {
  if (!value) return null
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  if (!match) return null

  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  const date = new Date(year, month - 1, day)
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) {
    return null
  }
  return { year, month, day }
}

function todayDate(today: Date): CalendarDate {
  return {
    year: today.getFullYear(),
    month: today.getMonth() + 1,
    day: today.getDate(),
  }
}

function calendarDay(date: CalendarDate): number {
  return Math.floor(Date.UTC(date.year, date.month - 1, date.day) / 86_400_000)
}

function daysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate()
}

export function getDaysTogether(value: string | null, today = new Date()): number | null {
  const start = parseDate(value)
  if (!start) return null
  return Math.max(0, calendarDay(todayDate(today)) - calendarDay(start) + 1)
}

export function getNextAnniversaryDays(value: string | null, today = new Date()): number | null {
  const anniversary = parseDate(value)
  if (!anniversary) return null

  const current = todayDate(today)
  let year = current.year
  let day = Math.min(anniversary.day, daysInMonth(year, anniversary.month))
  let candidate = calendarDay({ year, month: anniversary.month, day })
  const currentDay = calendarDay(current)

  if (candidate < currentDay) {
    year += 1
    day = Math.min(anniversary.day, daysInMonth(year, anniversary.month))
    candidate = calendarDay({ year, month: anniversary.month, day })
  }

  return candidate - currentDay
}
