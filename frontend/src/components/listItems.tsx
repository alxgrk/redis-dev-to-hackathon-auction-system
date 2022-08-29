import * as React from 'preact';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import ListSubheader from '@mui/material/ListSubheader';
import DashboardIcon from '@mui/icons-material/Dashboard';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import PanToolIcon from '@mui/icons-material/PanTool';
import CategoryIcon from '@mui/icons-material/Category';
import GavelIcon from '@mui/icons-material/Gavel';
import {Link} from "react-router-dom";

export const mainListItems = (
    <React.Fragment>
        <Link to={"/"}>
            <ListItemButton>
                <ListItemIcon>
                    <DashboardIcon/>
                </ListItemIcon>
                <ListItemText primary="Home"/>
            </ListItemButton>
        </Link>
        <Link to={"/search"}>
            <ListItemButton>
                <ListItemIcon>
                    <ShoppingCartIcon/>
                </ListItemIcon>
                <ListItemText primary="Search"/>
            </ListItemButton>
        </Link>
    </React.Fragment>
);

export const secondaryListItems = (
    <React.Fragment>
        <ListSubheader component="div" inset>
            Personal
        </ListSubheader>
        <ListItemButton>
            <ListItemIcon>
                <CategoryIcon/>
            </ListItemIcon>
            <ListItemText primary="Items"/>
        </ListItemButton>
        <ListItemButton>
            <ListItemIcon>
                <GavelIcon/>
            </ListItemIcon>
            <ListItemText primary="Auctions"/>
        </ListItemButton>
        <ListItemButton>
            <ListItemIcon>
                <PanToolIcon/>
            </ListItemIcon>
            <ListItemText primary="Bids"/>
        </ListItemButton>
    </React.Fragment>
);
