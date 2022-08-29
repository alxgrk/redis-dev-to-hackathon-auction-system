import React from 'react';
import {ComponentChildren} from 'preact';

import PopupWindow from './PopupWindow';
import {toQuery} from './utils';
import IconButton from "@mui/material/IconButton";
import GitHubIcon from "@mui/icons-material/GitHub";
import {AuthContext} from "../../main";
import {useContext} from "preact/compat";
import Typography from "@mui/material/Typography";
import AccountBoxIcon from '@mui/icons-material/AccountBox';
import Avatar from "@mui/material/Avatar";
import Box from "@mui/material/Box";

interface GitHubLoginProps {
    buttonText?: string;
    children?: ComponentChildren;
    className?: string;
    onRequest?: () => void;
    onSuccess: (data: any) => void;
    onFailure: (error: any) => void;
    redirectUri?: string;
}

const defaultProps: Omit<GitHubLoginProps, 'clientId'> = {
    buttonText: 'Sign in with GitHub',
    redirectUri: 'http://localhost:5173/',
    onRequest: () => {
    },
    onSuccess: () => {
    },
    onFailure: () => {
    },
}

export default function GitHubLogin(props: GitHubLoginProps) {

    props = {...defaultProps, ...props}

    const {state, dispatch} = useContext(AuthContext);

    const onBtnClick = () => {
        if (!state.isLoggedIn) {
            const {redirectUri} = props;
            const search = toQuery({
                redirect_uri: redirectUri,
            });
            const popup = PopupWindow.open(
                'github-oauth-authorize',
                `http://localhost:8080/login-github?${search}`,
                {height: 500, width: 600, left: 100, top: 100}
            );

            onRequest();
            popup.then(
                data => onSuccess(data),
                error => onFailure(error)
            );
        } else {
            console.log("logout")
            dispatch({
                type: "LOGOUT",
            })
        }
    }

    const onRequest = () => {
        props.onRequest();
    }

    const onSuccess = async (data) => {
        props.onSuccess(data);
        dispatch({
            type: "LOGIN",
            payload: {
                isLoggedIn: true,
            }
        })

        const user = await fetch(`http://localhost:8080/users/me`, {
            credentials: 'include'
        })
            .then(r => r.json())
        dispatch({
            type: "USER_INFO",
            payload: {
                user
            }
        })
    }

    const onFailure = (error) => {
        props.onFailure(error);
    }

    const {className, buttonText, children} = props;
    const attrs: any = {onClick: onBtnClick};

    if (className) {
        attrs.className = className;
    }

    return (
        <div>
            {!state.isLoggedIn
                ? <IconButton color="inherit" {...attrs}>
                    <GitHubIcon/>
                </IconButton>
                :
                (
                    state.user?.avatarUrl
                        ? <Box sx={{display: "inline-flex", verticalAlign: "middle", alignItems: "center"}} onClick={onBtnClick}>
                            <Avatar src={state.user.avatarUrl} alt={"Profile Image"} sx={{ width: 36, height: 36 }}/>
                            <Typography
                                        variant="h6"
                                        sx={{marginLeft: "0.4em"}}>Logout</Typography>
                        </Box>
                        : <IconButton color="inherit" {...attrs}>
                            <AccountBoxIcon/>
                            <Typography onClick={onBtnClick}
                                        variant="h6"
                                        sx={{marginLeft: "0.4em"}}>Logout</Typography>
                        </IconButton>
                )
            }
        </div>
    );
}
