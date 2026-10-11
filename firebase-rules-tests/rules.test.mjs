// Security Rules tests for firestore.rules and storage.rules — run with `npm test`
// (starts the Firestore + Storage emulators, runs this file, shuts them down).
import { before, after, beforeEach, describe, test } from 'node:test';
import { readFileSync } from 'node:fs';
import {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, deleteDoc, collection, query, where, getDocs } from 'firebase/firestore';
import { ref, uploadBytes, getBytes } from 'firebase/storage';

const ALICE = 'alice-uid';
const BOB = 'bob-uid';
const payload = (userId, data = '{"id":"r1"}') => ({ userId, data });

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-bugreel',
    firestore: { rules: readFileSync('../firestore.rules', 'utf8') },
    storage: { rules: readFileSync('../storage.rules', 'utf8') },
  });
});

after(async () => { await env.cleanup(); });

beforeEach(async () => {
  await env.clearFirestore();
  await env.clearStorage();
});

const db = (uid) => (uid ? env.authenticatedContext(uid) : env.unauthenticatedContext()).firestore();
const bucket = (uid) => (uid ? env.authenticatedContext(uid) : env.unauthenticatedContext()).storage();

async function seed(path, data) {
  await env.withSecurityRulesDisabled(async (ctx) => { await setDoc(doc(ctx.firestore(), path), data); });
}

for (const col of ['reports', 'outcome_feedback']) {
  describe(`firestore /${col}`, () => {
    test('signed-in owner can create, read, update and delete their own doc', async () => {
      const d = doc(db(ALICE), `${col}/r1`);
      await assertSucceeds(setDoc(d, payload(ALICE)));
      await assertSucceeds(getDoc(d));
      await assertSucceeds(setDoc(d, payload(ALICE, '{"id":"r1","v":2}')));
      await assertSucceeds(deleteDoc(d));
    });

    test('unauthenticated client can neither read nor write', async () => {
      await seed(`${col}/r1`, payload(ALICE));
      await assertFails(getDoc(doc(db(null), `${col}/r1`)));
      await assertFails(setDoc(doc(db(null), `${col}/r2`), payload('anonymous')));
    });

    test('another user cannot read, overwrite or delete the doc', async () => {
      await seed(`${col}/r1`, payload(ALICE));
      const d = doc(db(BOB), `${col}/r1`);
      await assertFails(getDoc(d));
      await assertFails(setDoc(d, payload(BOB)));
      await assertFails(deleteDoc(d));
    });

    test('cannot create a doc claiming someone else as owner', async () => {
      await assertFails(setDoc(doc(db(BOB), `${col}/r1`), payload(ALICE)));
    });

    test('rejects extra fields, missing fields, non-string data and oversize data', async () => {
      const d = doc(db(ALICE), `${col}/r1`);
      await assertFails(setDoc(d, { ...payload(ALICE), extra: true }));
      await assertFails(setDoc(d, { userId: ALICE }));
      await assertFails(setDoc(d, { userId: ALICE, data: 42 }));
      await assertFails(setDoc(d, payload(ALICE, 'x'.repeat(900001))));
    });

    test('owner-scoped query (pullAllReports) is allowed; unscoped query is denied', async () => {
      await seed(`${col}/r1`, payload(ALICE));
      await seed(`${col}/r2`, payload(BOB));
      const c = collection(db(ALICE), col);
      const snap = await assertSucceeds(getDocs(query(c, where('userId', '==', ALICE))));
      if (snap.size !== 1) throw new Error(`expected 1 doc, got ${snap.size}`);
      await assertFails(getDocs(c));
    });
  });
}

describe('firestore default deny', () => {
  test('unknown collections are closed even to signed-in users', async () => {
    await assertFails(setDoc(doc(db(ALICE), 'anything/x'), { a: 1 }));
    await assertFails(getDoc(doc(db(ALICE), 'users/alice-uid')));
  });
});

describe('storage evidence files', () => {
  const mp4 = { contentType: 'video/mp4' };
  const png = { contentType: 'image/png' };
  const bytes = new Uint8Array([1, 2, 3]);
  const base = (uid) => `users/${uid}/reports/r1`;

  test('owner can upload and read their recording and screenshots', async () => {
    const s = bucket(ALICE);
    await assertSucceeds(uploadBytes(ref(s, `${base(ALICE)}/recording.mp4`), bytes, mp4));
    await assertSucceeds(uploadBytes(ref(s, `${base(ALICE)}/screenshots/screenshot_0.png`), bytes, png));
    await assertSucceeds(getBytes(ref(s, `${base(ALICE)}/recording.mp4`)));
  });

  test('another user cannot read or write the owner\'s files', async () => {
    await assertSucceeds(uploadBytes(ref(bucket(ALICE), `${base(ALICE)}/recording.mp4`), bytes, mp4));
    await assertFails(getBytes(ref(bucket(BOB), `${base(ALICE)}/recording.mp4`)));
    await assertFails(uploadBytes(ref(bucket(BOB), `${base(ALICE)}/recording.mp4`), bytes, mp4));
  });

  test('unauthenticated client is denied', async () => {
    await assertFails(uploadBytes(ref(bucket(null), `${base(ALICE)}/recording.mp4`), bytes, mp4));
  });

  test('wrong content type or file name is rejected', async () => {
    const s = bucket(ALICE);
    await assertFails(uploadBytes(ref(s, `${base(ALICE)}/recording.mp4`), bytes, { contentType: 'text/html' }));
    await assertFails(uploadBytes(ref(s, `${base(ALICE)}/screenshots/evil.exe`), bytes, png));
    await assertFails(uploadBytes(ref(s, `${base(ALICE)}/other.bin`), bytes, mp4));
  });

  test('paths outside users/{uid}/reports are denied', async () => {
    await assertFails(uploadBytes(ref(bucket(ALICE), 'reports/r1/recording.mp4'), bytes, mp4));
  });
});
