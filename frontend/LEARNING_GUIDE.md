# React + TypeScript Learning Guide

Learn by building the frontend for your Net Worth Calculator API.

Each step introduces **one new concept**, gives you a goal, hints, and tells you
what to ask me if you get stuck. Work through them in order — each builds on the last.

**To start the dev server:**

```bash
cd frontend
npm run dev
```

Then open http://localhost:5173 in your browser. Changes auto-reload.

---

## Prerequisites Cheat Sheet

Before you begin, here are the key TypeScript/React ideas you'll use:

| Concept | One-liner |
|---------|-----------|
| **JSX** | HTML-like syntax inside JavaScript: `<h1>Hello</h1>` |
| **Component** | A function that returns JSX. That's it. |
| **Props** | Arguments passed to a component: `<Card title="Hi" />` |
| **State** | Data that, when changed, makes the component re-render |
| **Hook** | Special function (starts with `use`) that gives components superpowers |
| **Type** | TypeScript shape: `type User = { name: string; age: number }` |
| **Generic `<T>`** | "The caller decides the type": `function get<T>(url): Promise<T>` |

---

## Step 1 — Your First Component

**Concept:** Components, JSX, file structure

**Goal:** Create a separate `App.tsx` file and import it from `main.tsx`.

**What to do:**

1. Create `src/App.tsx` with a function `App` that returns some JSX (anything you want)
2. Export it: `export default function App() { ... }`
3. In `main.tsx`, replace the inline `App` function with: `import App from "./App"`
4. Check the browser — your new component should render

**Key ideas:**
- Every `.tsx` file can export components
- `export default` means "this is the main thing this file exports"
- `import X from "./file"` imports the default export (no curly braces)
- `import { X } from "./file"` imports a **named** export (with curly braces)

**When you're done:** You have the pattern for every file you'll create. Ask me to review!

---

## Step 2 — Props and TypeScript Types

**Concept:** Passing data to components, TypeScript type annotations

**Goal:** Build a reusable `StatusBadge` component that shows a label and status.

**What to do:**

1. Create `src/components/StatusBadge.tsx`
2. Define a type for the props:
   ```tsx
   type StatusBadgeProps = {
     label: string;
     ok: boolean;
   };
   ```
3. Write the component:
   ```tsx
   export function StatusBadge({ label, ok }: StatusBadgeProps) {
     return <span>{label}: {ok ? "Connected" : "Down"}</span>;
   }
   ```
4. Use it in `App.tsx`: `<StatusBadge label="Database" ok={true} />`

**Key ideas:**
- Props are destructured in the function signature: `{ label, ok }`
- TypeScript enforces the shape — if you forget `ok`, you get a compile error
- `{expression}` inside JSX evaluates JavaScript: `{ok ? "yes" : "no"}`
- The ternary operator `condition ? valueIfTrue : valueIfFalse` is how you do
  inline conditionals in JSX (no if/else inside JSX)

**Stretch:** Add an optional `loading` prop (`loading?: boolean`) and show "..." when loading.

---

## Step 3 — State with useState

**Concept:** React state, re-rendering, event handlers

**Goal:** Build a counter component, then a login/register toggle.

**What to do:**

1. Create `src/components/Counter.tsx`:
   ```tsx
   import { useState } from "react";

   export function Counter() {
     const [count, setCount] = useState(0);
     //      ^value  ^setter         ^initial value

     return (
       <div>
         <p>Count: {count}</p>
         <button onClick={() => setCount(count + 1)}>+1</button>
         <button onClick={() => setCount(0)}>Reset</button>
       </div>
     );
   }
   ```
2. Add it to `App.tsx` and click the buttons. Notice the number updates!
3. Now create a toggle: `const [isRegister, setIsRegister] = useState(false)`
   and show different text based on its value

**Key ideas:**
- `useState` returns `[currentValue, setterFunction]` — this is called array destructuring
- When you call `setCount(newValue)`, React re-renders the component with the new value
- `onClick={() => doSomething()}` — the arrow function is needed so it runs on click, not immediately
- State is **per component instance** — two `<Counter />` components have separate counts

**Think about:** In Scala terms, `useState` is like a `Ref[F, A]` — it holds a value and
notifying the "runtime" (React) when it changes.

---

## Step 4 — Domain Types

**Concept:** TypeScript types for your API, mirroring Scala case classes

**Goal:** Create `src/api/types.ts` with the types your backend returns.

**What to do:**

Create the file with these types (look at your Scala `domain/` package for reference):

```typescript
export type AssetType = "Loan" | "Cash" | "Investment" | "Property";

export type Asset = {
  assetId: number;
  assetType: AssetType;
  assetName: string;
  userId: number;
};

export type CreateAssetRequest = {
  assetType: string;
  assetName: string;
};

// Add more types: Transaction, CreateTransactionRequest,
// TransactionValue, AggregatedTransactions, AppStatus, NetWorthFilters
// Look at your Scala case classes to figure out the shapes!
```

Also add an error class:
```typescript
export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
    this.name = "ApiError";
  }
}
```

**Key ideas:**
- `type X = "A" | "B"` is a **union type** — like a Scala sealed trait with case objects
- `export type` makes it importable from other files
- `class` is used when you need `instanceof` checks (like for errors)
- `?` makes a field optional: `year?: number` means it can be undefined

**Think about:** `type` vs Scala: `type Asset = { ... }` is like
`case class Asset(assetId: Int, assetType: AssetType, ...)` — a product type.

---

## Step 5 — HTTP Client (fetch wrapper)

**Concept:** async/await, fetch API, generics, localStorage

**Goal:** Build `src/api/client.ts` — a thin wrapper around the browser's `fetch`.

**What to do:**

1. Create `src/api/client.ts`
2. Implement token storage:
   ```typescript
   let token: string | null = localStorage.getItem("jwt");

   export function setToken(newToken: string | null) {
     token = newToken;
     if (newToken) localStorage.setItem("jwt", newToken);
     else localStorage.removeItem("jwt");
   }

   export function getToken(): string | null {
     return token;
   }
   ```
3. Implement the core request function:
   ```typescript
   async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
     const headers: Record<string, string> = {
       "Content-Type": "application/json",
     };
     if (token) headers["Authorization"] = `Bearer ${token}`;

     const response = await fetch(`http://localhost:9000/v1${path}`, {
       ...options,
       headers,
     });

     if (response.status === 204) return undefined as T;
     if (!response.ok) throw new ApiError(response.status, await response.text());
     return response.json() as Promise<T>;
   }
   ```
4. Export convenience helpers: `get<T>`, `post<T>`, `del<T>`

**Key ideas:**
- `async/await` is like Scala's `flatMap` chains but with sugar — `await` "unwraps" a Promise
- `<T>` generic means the caller decides the return type: `get<Asset[]>("/assets")`
- `fetch` is the browser's built-in HTTP client — no library needed
- `Record<string, string>` is TypeScript for `Map[String, String]`
- The spread operator `...options` merges objects (like Scala's `copy()`)

**Test it:** In App.tsx, try calling the health endpoint:
```tsx
import { get } from "./api/client";
// In your component, maybe on a button click:
const data = await get<{ postgres: boolean; redis: boolean }>("/healthcheck");
```

---

## Step 6 — API Modules

**Concept:** Organizing API calls, importing types

**Goal:** Create `src/api/health.ts`, `src/api/auth.ts`, `src/api/assets.ts`, `src/api/transactions.ts`

**What to do:**

Start simple with health:
```typescript
// src/api/health.ts
import { get } from "./client";
import type { AppStatus } from "./types";

export function fetchHealth(): Promise<AppStatus> {
  return get<AppStatus>("/healthcheck");
}
```

Then auth (login, register, logout), assets (fetch, create, delete),
and transactions (fetch, create, delete, fetchNetWorth).

**Key ideas:**
- `import type { X }` imports only the type — it's erased at runtime (like Scala's type-only imports)
- Each function is a thin wrapper: call the HTTP client with the right path and types
- For auth: after login/register, call `setToken(token)` to store the JWT
- For transactions: you'll need a helper to build query strings from filter objects

**Pattern:** Every API module follows the same shape — import client helpers,
import types, export typed functions. Keep them small.

---

## Step 7 — React Context (Shared State)

**Concept:** Context API, custom hooks, useCallback

**Goal:** Build `src/context/AuthContext.tsx` so any component can access auth state.

This is the most conceptually dense step. Take it slow.

**The problem:** Multiple components need to know "is the user logged in?" and call
login/logout. Passing this through props at every level is painful ("prop drilling").

**The solution:** React Context — a way to share state across the component tree.

**What to do:**

1. Define the shape of your context:
   ```tsx
   type AuthContextValue = {
     isAuthenticated: boolean;
     login: (username: string, password: string) => Promise<void>;
     register: (username: string, password: string) => Promise<void>;
     logout: () => Promise<void>;
   };
   ```
2. Create the context: `const AuthContext = createContext<AuthContextValue | undefined>(undefined)`
3. Create a Provider component that wraps children and provides the value
4. Create a custom hook `useAuth()` that reads from the context

**Key ideas:**
- `createContext` creates the "mailbox" — `useContext` reads from it
- The Provider fills the mailbox: `<AuthContext.Provider value={...}>{children}</AuthContext.Provider>`
- `useCallback` memoizes functions so they don't get recreated every render
  (like caching a lambda — React re-runs your function on every render)
- The custom hook pattern (`useAuth`) is just a convenience — it's `useContext` + a null check

**Think about:** This is like Scala's `Reader` monad or dependency injection —
you define what's available at the top, and any nested component can access it.

---

## Step 8 — React Router (Navigation)

**Concept:** Client-side routing, protected routes, layouts

**Goal:** Set up page navigation with login protection.

**What to do:**

1. Create `src/components/ProtectedRoute.tsx`:
   ```tsx
   import { Navigate, Outlet } from "react-router";
   import { useAuth } from "../context/AuthContext";

   export function ProtectedRoute() {
     const { isAuthenticated } = useAuth();
     if (!isAuthenticated) return <Navigate to="/login" replace />;
     return <Outlet />;
   }
   ```
2. Create `src/components/Layout.tsx` with a nav bar and `<Outlet />` for page content
3. In `App.tsx`, set up the router:
   ```tsx
   <BrowserRouter>
     <Routes>
       <Route path="/login" element={<LoginPage />} />
       <Route element={<ProtectedRoute />}>
         <Route element={<Layout />}>
           <Route path="/" element={<DashboardPage />} />
           <Route path="/assets" element={<AssetsPage />} />
         </Route>
       </Route>
     </Routes>
   </BrowserRouter>
   ```

**Key ideas:**
- `<Route>` maps a URL path to a component
- `<Outlet>` is a placeholder — "render the child route's component here"
- Layout routes (no `path`) wrap child routes: `ProtectedRoute` checks auth,
  `Layout` adds the nav bar
- `<Link to="/assets">` navigates without a full page reload (unlike `<a href>`)
- `<Navigate>` redirects programmatically

**Start simple:** Create placeholder pages that just show their name (`<h1>Dashboard</h1>`).
You'll fill them in next.

---

## Step 9 — Forms (Login Page)

**Concept:** react-hook-form, form validation, error handling

**Goal:** Build `src/pages/LoginPage.tsx` with login and register forms.

**What to do:**

1. Use `useForm<FormData>()` from react-hook-form:
   ```tsx
   type FormData = { username: string; password: string };

   const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>();
   ```
2. Connect inputs with the spread operator: `<input {...register("username", { required: "Email is required" })} />`
3. Handle submit: `<form onSubmit={handleSubmit(onSubmit)}>`
4. Show validation errors from `errors.username?.message`
5. Add a toggle between login and register mode

**Key ideas:**
- `register("fieldName")` returns `{ onChange, onBlur, ref, name }` — spread puts them on the input
- `handleSubmit(fn)` calls your `fn` only if validation passes
- `isSubmitting` is true while your async `onSubmit` is running — use it to disable the button
- `react-hot-toast` gives you `toast.success("Done!")` and `toast.error("Oops")` notifications

**Test it:** Start the backend (`sbt run` + `docker-compose up -d`), register a user, and
verify the JWT gets stored in localStorage (check browser DevTools → Application → Local Storage).

---

## Step 10 — Data Fetching with TanStack Query

**Concept:** useQuery for GET requests, loading/error states, caching

**Goal:** Build `src/pages/DashboardPage.tsx` that shows health status and asset count.

**What to do:**

1. Wrap your app with `<QueryClientProvider>` in App.tsx (if not done yet)
2. Use `useQuery` to fetch data:
   ```tsx
   const health = useQuery({
     queryKey: ["health"],      // unique ID for caching
     queryFn: fetchHealth,      // the async function to call
   });

   // health.isLoading — true while fetching
   // health.data — the response (undefined until loaded)
   // health.isError — true if the request failed
   ```
3. Render different UI for loading, error, and success states

**Key ideas:**
- `queryKey` identifies the query — same key = same cache. Change the key, it refetches.
- TanStack Query handles caching, deduplication, and background refetching automatically
- You can use multiple `useQuery` calls in one component — they fetch in parallel
- Optional chaining (`health.data?.postgres`) safely accesses nested values

**Think about:** `useQuery` is like a managed `Resource[F, A]` — it handles the lifecycle
(loading, success, error, refetch) so you don't have to.

---

## Step 11 — Mutations (CRUD)

**Concept:** useMutation for POST/DELETE, cache invalidation

**Goal:** Build `src/pages/AssetsPage.tsx` with a list, create form, and delete button.

**What to do:**

1. Use `useQuery` to fetch and display the asset list
2. Use `useMutation` for create and delete:
   ```tsx
   const createMutation = useMutation({
     mutationFn: createAsset,
     onSuccess: () => {
       queryClient.invalidateQueries({ queryKey: ["assets"] });
       toast.success("Asset created");
       reset(); // clear the form
     },
   });
   ```
3. Call it: `createMutation.mutate(formData)`
4. Use `createMutation.isPending` to show loading state on the button

**Key ideas:**
- `useMutation` is for write operations (POST, PUT, DELETE)
- After a mutation succeeds, `invalidateQueries` tells TanStack Query to refetch
  the affected data — this keeps your UI in sync with the server
- `useQueryClient()` gives you access to the query cache from any component
- Combine `useForm` (Step 9) + `useMutation` (this step) for full CRUD forms

---

## Step 12 — Complex Page (Transactions)

**Concept:** Combining everything, local state + server state, query parameters

**Goal:** Build `src/pages/TransactionsPage.tsx` with filters, create form, and list.

This page combines all previous concepts:
- `useState` for filter values (year, currency)
- `useQuery` with dynamic `queryKey` that includes filters (re-fetches when filters change)
- `useMutation` for create/delete
- `useForm` for the transaction form
- Multiple API calls in one component

**Key challenge:** The query key should include your filter state:
```tsx
const netWorth = useQuery({
  queryKey: ["netWorth", yearFilter, currencyFilter],
  queryFn: () => fetchNetWorth({ year: yearFilter, currency: currencyFilter }),
});
```
When `yearFilter` changes, TanStack Query automatically re-fetches!

---

## Step 13 — Styling with Tailwind

**Concept:** Utility-first CSS, responsive design

Tailwind is already configured. Instead of writing CSS files, you add classes directly:

```tsx
// Before (plain HTML)
<div>
  <h1>Title</h1>
</div>

// After (Tailwind)
<div className="mx-auto max-w-6xl px-4 py-8">
  <h1 className="text-3xl font-bold text-gray-800">Title</h1>
</div>
```

**Common patterns:**
- `p-4` = padding, `px-4` = horizontal padding, `py-2` = vertical padding
- `text-sm/lg/xl/2xl/3xl` = font sizes
- `bg-gray-50`, `text-gray-800` = colors
- `rounded-lg shadow` = rounded corners + drop shadow
- `flex items-center gap-4` = flexbox layout
- `grid grid-cols-3` = CSS grid

Go back through your pages and make them look good!

---

## Step 14 — Charts (Bonus)

**Concept:** Recharts library for data visualization

**Goal:** Add a bar chart to the Dashboard showing net worth over time.

```tsx
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";

// Transform your data into chart format:
const chartData = netWorthData.map(entry => ({
  period: `${entry.year}-${String(entry.month).padStart(2, "0")}`,
  total: entry.totals.join(", "),
}));

// Render:
<ResponsiveContainer width="100%" height={300}>
  <BarChart data={chartData}>
    <CartesianGrid strokeDasharray="3 3" />
    <XAxis dataKey="period" />
    <YAxis />
    <Tooltip />
    <Bar dataKey="total" fill="#3b82f6" />
  </BarChart>
</ResponsiveContainer>
```

---

## How to Use This Guide

1. **Work through steps in order** — each builds on the last
2. **Type the code yourself** — don't copy-paste. Muscle memory matters.
3. **Break things on purpose** — remove a prop, misspell a type, skip an await. See what happens.
4. **Ask me anything** — say "I'm on Step 5 and stuck on X" and I'll help without spoiling later steps
5. **Run `npm run dev`** and keep the browser open — you'll see changes instantly

### Useful commands

```bash
cd frontend
npm run dev          # start dev server (hot reload)
npm run build        # type-check + production build
npm run lint         # check for common mistakes
```

### Useful browser DevTools (F12)

- **Console** — see errors and `console.log()` output
- **Network** — see HTTP requests to your backend
- **Application → Local Storage** — see the stored JWT token
- **Components** (React DevTools extension) — inspect component tree and state
