import { readFile } from "node:fs/promises";

export interface HolidayYear {
  off: string[];
  work: string[];
}

export interface HolidayCalendar {
  source: string;
  years: Record<string, HolidayYear>;
}

const EMPTY: HolidayCalendar = { source: "", years: {} };

export function dayKeyOf(at: number): string {
  const date = new Date(at);
  const month = `${date.getUTCMonth() + 1}`.padStart(2, "0");
  const day = `${date.getUTCDate()}`.padStart(2, "0");
  return `${date.getUTCFullYear()}-${month}-${day}`;
}

export async function loadHolidays(path: string): Promise<HolidayCalendar> {
  try {
    const raw = JSON.parse(await readFile(path, "utf8")) as HolidayCalendar;
    if (!raw || typeof raw !== "object" || !raw.years) return EMPTY;
    return raw;
  } catch {
    return EMPTY;
  }
}

export function isOffDay(calendar: HolidayCalendar, at: number): boolean {
  const key = dayKeyOf(at);
  const year = calendar.years[key.slice(0, 4)];
  return year ? year.off.includes(key) : false;
}

export function isMakeupWorkday(calendar: HolidayCalendar, at: number): boolean {
  const key = dayKeyOf(at);
  const year = calendar.years[key.slice(0, 4)];
  return year ? year.work.includes(key) : false;
}
