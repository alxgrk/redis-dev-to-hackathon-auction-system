import {AuthAction} from "../store/reducer";

const { fetch: originalFetch } = window;

export const registerUnauthorizedInterceptor = (dispatch: (AuthAction) => void) => {
    window.fetch = async (...args) => {
        const [resource, config] = args;

        // noinspection TypeScriptValidateTypes
        let response = await originalFetch(resource, config);

        if (response.status === 401) {
            const refresh = await fetch(`http://localhost:8080/token/refresh`, {
                credentials: 'include'
            })
            console.log(`Refreshing token was ${refresh.ok ? "successful" : "unsuccessful"}`)
            if (refresh.ok) {
                dispatch({
                    type: "LOGIN",
                    payload: {
                        isLoggedIn: true,
                    }
                })

                // noinspection TypeScriptValidateTypes
                response = await originalFetch(resource, config);
            } else {
                dispatch({
                    type: "LOGOUT"
                })
            }
        }

        return response;
    };
}
