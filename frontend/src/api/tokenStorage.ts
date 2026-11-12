const ACCESS_KEY = 'accessToken';
const REFRESH_KEY = 'refreshToken';

export const tokenStorage = {
    getAccess: () => localStorage.getItem(ACCESS_KEY),
    getRefresh: () => localStorage.getItem(REFRESH_KEY),
    set: (access: string, refresh: string) => {
        localStorage.setItem(ACCESS_KEY, access);
        localStorage.setItem(REFRESH_KEY, refresh);
    },
    clear: () => {
        localStorage.removeItem(ACCESS_KEY);
        localStorage.removeItem(REFRESH_KEY);
    }
}