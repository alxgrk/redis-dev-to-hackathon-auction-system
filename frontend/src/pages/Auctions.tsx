import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import Container from "@mui/material/Container";
import Grid from "@mui/material/Grid";
import {Copyright} from "../components/Copyright";
import SingleAuction from "../components/SingleAuction";
import {useEffect, useState} from "preact/compat";
import {useMediaQuery} from "@mui/material";
import {useTheme} from "@mui/material/styles";
import SearchBar from "material-ui-search-bar";
import {useLocation} from "react-router-dom";
import flatMap from "lodash/flatMap"
import flatten from "lodash/flatten"
import ScrollableBox, {useDefaultLipClassNames} from "react-scrollable-box";

export default function Auctions() {

    const theme = useTheme()
    const lipClassNames = useDefaultLipClassNames();
    const {search} = useLocation();
    const queryParams = new URLSearchParams(search)

    const [items, setItems] = useState([])
    const [auctions, setAuctions] = useState([])
    const [sellers, setSellers] = useState([])

    useEffect(() => {
        const fetchAuctions = async () => {
            const auctions = await fetch(`http://localhost:8080/auctions${queryParams.get("items") ? `?items=${queryParams.get("items")}` : ""}`, {
                credentials: 'include'
            })
                .then(r => r.json())
                .then(r => Array.isArray(r) ? r : [r])
            setAuctions(auctions)
            return auctions
        }
        fetchAuctions().then(r => console.log("Fetched auctions:", r));
    }, [])

    useEffect(() => {
        const fetchItems = async () => {
            const itemIds = flatMap(auctions, (a) => a.items);
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
    }, [auctions])

    useEffect(() => {
        const fetchSellers = async () => {
            const newSellers = await Promise.all(auctions
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
            <Container>
                <Grid
                    container
                    spacing={{xs: 2, md: 3}}
                    columns={{xs: 2, sm: 8, md: 12}}
                >
                    {auctions.map(auction =>
                        <Grid item key={auction.id} xs={2} sm={4} md={4} display="flex" flexDirection={'column'}
                              alignItems="center">
                            <SingleAuction auction={auction}
                                           seller={sellers.find(({id}) => auction.seller === id)}
                                           items={items.filter(({id}) => auction.items.indexOf(id) !== -1)}/>
                        </Grid>
                    )}
                </Grid>
            </Container>
            <Copyright sx={{pt: 4}}/>
        </Container>
    </ScrollableBox>)
}
