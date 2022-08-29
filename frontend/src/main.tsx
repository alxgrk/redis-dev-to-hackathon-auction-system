import {createContext, render} from 'preact'
import {useEffect, useReducer} from "preact/compat";
import './index.css'
import Dashboard from "./Dashboard";
import {AuthAction, initialState, reducer} from "./store/reducer";
import {BrowserRouter} from "react-router-dom";
import {registerUnauthorizedInterceptor} from "./network/fetch-interceptor";

export const AuthContext = createContext({
    state: initialState,
    dispatch: (action: AuthAction) => {
    }
});

function Main() {
    const [state, dispatch] = useReducer(reducer, initialState);

    registerUnauthorizedInterceptor(dispatch)

    return (
        <BrowserRouter>
            <AuthContext.Provider
                value={{
                    state,
                    dispatch
                }}>
                <Dashboard/>
            </AuthContext.Provider>
        </BrowserRouter>
    )
}

render(
    <Main/>,
    document.getElementById('app') as HTMLElement
)
