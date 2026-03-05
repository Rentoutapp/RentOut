import { initializeApp, FirebaseApp } from 'firebase/app';
import { getAuth, Auth } from 'firebase/auth';
import { getFirestore, Firestore } from 'firebase/firestore';
import { getStorage, FirebaseStorage } from 'firebase/storage';

// ---------------------------------------------------------------------------
// Firebase configuration — values are injected at build time by Vite from the
// .env file. Never hard-code secrets directly in source files.
// ---------------------------------------------------------------------------
const firebaseConfig = {
  apiKey:            import.meta.env.VITE_FIREBASE_API_KEY            as string,
  authDomain:        import.meta.env.VITE_FIREBASE_AUTH_DOMAIN        as string,
  databaseURL:       import.meta.env.VITE_FIREBASE_DATABASE_URL       as string,
  projectId:         import.meta.env.VITE_FIREBASE_PROJECT_ID         as string,
  storageBucket:     import.meta.env.VITE_FIREBASE_STORAGE_BUCKET     as string,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID as string,
  appId:             import.meta.env.VITE_FIREBASE_APP_ID             as string,
};

// Initialise once — exported so every module shares the same app instance.
const app: FirebaseApp = initializeApp(firebaseConfig);

export const auth: Auth               = getAuth(app);
export const db: Firestore            = getFirestore(app);
export const storage: FirebaseStorage = getStorage(app);

export default app;
