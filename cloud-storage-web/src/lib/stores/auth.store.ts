import { createStore } from 'zustand/vanilla';
import type { User } from '$lib/api/types';
import { apiRequest } from '$lib/api/client';

interface AuthStoreState {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  fetchUser: () => Promise<void>;
  setUser: (user: User | null) => void;
  logout: () => Promise<void>;
}

export const authStore = createStore<AuthStoreState>((set) => ({
  user: null,
  isLoading: true,
  isAuthenticated: false,

  setUser: (user) => set({ user, isAuthenticated: Boolean(user), isLoading: false }),

  fetchUser: async () => {
    set({ isLoading: true });
    try {
      const data = await apiRequest('/users/me');
      set({ user: data.user, isAuthenticated: true, isLoading: false });
      if (data?.user?.email && typeof window !== 'undefined') {
        localStorage.setItem(
          'zacode_last_google_user',
          JSON.stringify({
            email: data.user.email,
            name: data.user.profile?.displayName || data.user.username,
            avatarUrl: data.user.profile?.avatarUrl,
          })
        );
        localStorage.removeItem('rea_last_google_user');
      }
    } catch {
      set({ user: null, isAuthenticated: false, isLoading: false });
    }
  },

  logout: async () => {
    try {
      await apiRequest('/auth/logout', { method: 'POST' });
    } finally {
      set({ user: null, isAuthenticated: false });
      window.location.href = '/login';
    }
  },
}));

