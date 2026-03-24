import React from "react";
import ReactDOM from "react-dom/client";
import CssBaseline from "@mui/material/CssBaseline";
import { ThemeProvider, createTheme } from "@mui/material/styles";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider } from "@tanstack/react-router";
import { router } from "./router";
import { AuthProvider } from "./lib/session";
import { brandTokens } from "./lib/brand";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false
    }
  }
});

const theme = createTheme({
  palette: {
    mode: "light",
    primary: {
      main: brandTokens.colors.shellDark
    },
    secondary: {
      main: brandTokens.colors.headerAccent
    },
    background: {
      default: brandTokens.colors.page,
      paper: brandTokens.colors.panel
    },
    text: {
      primary: brandTokens.colors.title,
      secondary: brandTokens.colors.shellText
    }
  },
  shape: {
    borderRadius: 8
  },
  typography: {
    fontFamily: '"Instrument Sans", "Segoe UI", "Helvetica Neue", Arial, sans-serif',
    h3: {
      fontSize: "2.2rem",
      fontWeight: 700
    },
    h4: {
      fontSize: "1.75rem",
      fontWeight: 700
    },
    h5: {
      fontSize: "1.3rem",
      fontWeight: 700
    },
    button: {
      fontWeight: 700,
      letterSpacing: 0
    }
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          backgroundColor: brandTokens.colors.page
        }
      }
    },
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: "none",
          borderRadius: 8
        }
      }
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: "none"
        }
      }
    },
    MuiTextField: {
      defaultProps: {
        size: "small"
      }
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 8
        }
      }
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottom: `1px solid ${brandTokens.colors.border}`
        },
        head: {
          color: brandTokens.colors.shellText,
          fontWeight: 700
        }
      }
    },
    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: 12,
          border: `1px solid ${brandTokens.colors.border}`,
          boxShadow: brandTokens.shadow
        }
      }
    }
  }
});

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <RouterProvider router={router} />
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>
  </React.StrictMode>
);
