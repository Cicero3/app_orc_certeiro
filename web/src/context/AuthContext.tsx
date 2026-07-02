import React, { createContext, useContext, useState, useEffect } from 'react';

// Domain Types
export interface User {
  userId: string;
  email: string;
}

export interface AuthResponse {
  userId: string;
  email: string;
  accessToken: string;
  expiresIn: number;
}

export interface ApiResponse<T> {
  data: T;
  error?: string;
  message?: string;
}

interface AuthContextData {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (data: AuthResponse) => void;
  logout: () => void;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextData>({} as AuthContextData);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Attempt to restore session from memory/storage on load
    const storedToken = localStorage.getItem('@App:token');
    const storedUser = localStorage.getItem('@App:user');
    
    if (storedToken && storedUser) {
      setToken(storedToken);
      setUser(JSON.parse(storedUser));
    }
    
    setIsLoading(false);
  }, []);

  const login = (authData: AuthResponse) => {
    const userData: User = { userId: authData.userId, email: authData.email };
    setUser(userData);
    setToken(authData.accessToken);
    
    // Storing Access Token (In a real high-security environment, we'd keep this only in memory, 
    // but we use localStorage for persistence across reloads in this MVP). 
    // The Refresh Token is kept safe as an HTTP-only cookie by the backend!
    localStorage.setItem('@App:token', authData.accessToken);
    localStorage.setItem('@App:user', JSON.stringify(userData));
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('@App:token');
    localStorage.removeItem('@App:user');
    
    // Call backend to invalidate refresh token cookie
    fetch('http://localhost:8080/api/v1/auth/logout', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }).catch(console.error);
  };

  return (
    <AuthContext.Provider value={{ user, token, isAuthenticated: !!user, login, logout, isLoading }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
