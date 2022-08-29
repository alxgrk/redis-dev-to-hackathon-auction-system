export const initialState: AuthState = {
    isLoggedIn: JSON.parse(localStorage.getItem("isLoggedIn")) || false,
    user: JSON.parse(localStorage.getItem("user")) || null,
};

export type AuthState = {
    isLoggedIn?: boolean,
    user?: any
};
export type AuthAction = { type: "LOGIN" | "USER_INFO" | "LOGOUT", payload?: AuthState };

export const reducer = (state: AuthState, action: AuthAction) => {
    switch (action.type) {
        case "LOGIN": {
            localStorage.setItem("isLoggedIn", JSON.stringify(action.payload.isLoggedIn))
            return {
                ...state,
                isLoggedIn: action.payload.isLoggedIn,
            };
        }
        case "USER_INFO": {
            localStorage.setItem("user", JSON.stringify(action.payload.user))
            return {
                ...state,
                user: action.payload.user
            };
        }
        case "LOGOUT": {
            localStorage.clear()
            return {
                ...state,
                isLoggedIn: false,
                user: null
            };
        }
        default:
            return state;
    }
};
