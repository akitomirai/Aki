const STORAGE_KEY = 'field_entry_drafts_v1'

function readStore() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch (error) {
    return {}
  }
}

function writeStore(store) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(store))
}

function getUserKey(user = {}) {
  return String(user?.id ?? user?.username ?? 'anonymous')
}

function getDraftKey(user, batchId) {
  return `${getUserKey(user)}:${String(batchId)}`
}

export function listFieldDrafts(user) {
  const userKey = getUserKey(user)
  return Object.values(readStore())
    .filter((item) => item.userKey === userKey)
    .sort((left, right) => Date.parse(right.updatedAt || 0) - Date.parse(left.updatedAt || 0))
}

export function getFieldDraft(user, batchId) {
  return readStore()[getDraftKey(user, batchId)] ?? null
}

export function saveFieldDraft(user, batchId, payload) {
  const store = readStore()
  const key = getDraftKey(user, batchId)
  const nextDraft = {
    id: key,
    userKey: getUserKey(user),
    batchId: String(batchId),
    updatedAt: new Date().toISOString(),
    data: payload
  }
  store[key] = nextDraft
  writeStore(store)
  return nextDraft
}

export function removeFieldDraft(user, batchId) {
  const store = readStore()
  delete store[getDraftKey(user, batchId)]
  writeStore(store)
}
