import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import Container from "@mui/material/Container";
import Grid from "@mui/material/Grid";
import Paper from "@mui/material/Paper";
import Chart from "../components/Chart";
import Deposits from "../components/Deposits";
import Orders from "../components/Orders";
import {Copyright} from "../components/Copyright";
import SingleAuction from "../components/SingleAuction";
import ScrollableBox, {useDefaultLipClassNames} from "react-scrollable-box";
import {useTheme} from "@mui/material/styles";
import flatMap from "lodash/flatMap"
import flatten from "lodash/flatten"
import {useContext, useEffect, useState} from "preact/compat";
import Typography from "@mui/material/Typography";
import List from "@mui/material/List";
import {ListItem} from "@mui/material";
import SingleItem from "../components/SingleItem";
import {AuthContext} from "../main";
import IconButton from "@mui/material/IconButton";
import GitHubIcon from "@mui/icons-material/GitHub";
import Avatar from "@mui/material/Avatar";
import AccountBoxIcon from "@mui/icons-material/AccountBox";


export default function Main() {
    const theme = useTheme()
    const lipClassNames = useDefaultLipClassNames();

    const {state, dispatch} = useContext(AuthContext);

    const [items, setItems] = useState([])
    const [myItems, setMyItems] = useState([])
    const [myAuctions, setMyAuctions] = useState([])
    const [sellers, setSellers] = useState([])

    useEffect(() => {
        const fetchAuctions = async () => {
            const auctions = await fetch(`http://localhost:8080/auctions/my`, {
                credentials: 'include'
            })
                .then(r => r.json())
                .then(r => Array.isArray(r) ? r : [r])
            setMyAuctions(auctions)
            return auctions
        }
        fetchAuctions().then(r => console.log("Fetched auctions:", r));
    }, [])

    useEffect(() => {
        const fetchItems = async () => {
            const itemIds = flatMap(myAuctions, (a) => a.items);
            const items = flatten(await Promise.all(itemIds.map(itemId =>
                fetch(`http://localhost:8080/items/${itemId}`, {
                    credentials: 'include'
                })
                    .then(r => r.json())
                    .then(r => Array.isArray(r) ? r : [r])
            )));
            setItems(items)
            return items
        }
        fetchItems().then(r => console.log("Fetched items:", r));
    }, [myAuctions])

    useEffect(() => {
        const fetchSellers = async () => {
            const newSellers = await Promise.all(myAuctions
                .filter(({seller}) => !sellers.find(({id}) => id === seller))
                .map(({seller}) =>
                    fetch(`http://localhost:8080/users/${seller}`, {
                        credentials: 'include'
                    }).then(r => r.json())
                )
            )
            setSellers([...sellers, ...newSellers])
            return sellers
        }
        fetchSellers().then(r => console.log("Fetched sellers:", r));
    }, [items])

    useEffect(() => {
        const fetchMyItems = async () => {
            const myItems = await fetch(`http://localhost:8080/items/my`, {
                credentials: 'include'
            })
                .then(r => r.json())
                .then(r => Array.isArray(r) ? r : [r])
            setMyItems(myItems)
            return myItems
        }
        fetchMyItems().then(r => console.log("Fetched my items:", r));
    }, [])

    return (<ScrollableBox
        {...lipClassNames}
        style={{
            maxHeight: '100vh', width: "100%", overflow: 'auto',
            backgroundColor: () =>
                theme.palette.mode === 'light'
                    ? theme.palette.grey[100]
                    : theme.palette.grey[900],
        }}
        component="main"
    >
        <Toolbar/>
        <Container sx={{mt: 4, mb: 4}}>
            {!state.isLoggedIn
                ? <IconButton color="inherit" onClick={console.log}>
                    <GitHubIcon/>
                    <Typography variant="h6"
                                sx={{marginLeft: "0.4em"}}>Login with Github</Typography>
                </IconButton>
                :
                (<Container>
                        <Container>
                            <Typography component='h3' variant='h3' textAlign='start' gutterBottom>
                                My Auctions
                            </Typography>

                            <ScrollableBox
                                {...lipClassNames}
                                style={{
                                    overflow: 'auto',
                                    backgroundColor: () =>
                                        theme.palette.mode === 'light'
                                            ? theme.palette.grey[100]
                                            : theme.palette.grey[900],
                                }}
                                component="main"
                            >
                                <List
                                    sx={{mb: "2em"}}
                                    display={"flex"}
                                    flexDirection={"row"}
                                >
                                    {myAuctions.map(auction =>
                                        <ListItem>
                                            <SingleAuction auction={auction}
                                                           seller={sellers.find(({id}) => auction.seller === id)}
                                                           items={items.filter(({id}) => auction.items.indexOf(id) !== -1)}
                                                           minMaxWidth={[240, 280]}
                                            />
                                        </ListItem>
                                    )}
                                </List>
                            </ScrollableBox>
                        </Container>
                        <Container>
                            <Typography component='h3' variant='h3' textAlign='start' gutterBottom>
                                My Items
                            </Typography>

                            <ScrollableBox
                                {...lipClassNames}
                                style={{
                                    overflow: 'auto',
                                    backgroundColor: () =>
                                        theme.palette.mode === 'light'
                                            ? theme.palette.grey[100]
                                            : theme.palette.grey[900],
                                }}
                                component="main"
                            >
                                <List
                                    sx={{mb: "2em"}}
                                    display={"flex"}
                                    flexDirection={"row"}
                                >
                                    {myItems.map(item =>
                                        <ListItem>
                                            <SingleItem item={item} owner={this.state.user}
                                                        minMaxWidth={[240, 280]}/>
                                        </ListItem>
                                    )}
                                </List>
                            </ScrollableBox>
                        </Container>
                    </Container>
                )}
            <Copyright sx={{pt: 4}}/>
        </Container>
    </ScrollableBox>)
}
