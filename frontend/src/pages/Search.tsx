import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import Container from "@mui/material/Container";
import Grid from "@mui/material/Grid";
import {Copyright} from "../components/Copyright";
import SingleItem from "../components/SingleItem";
import {useEffect, useState} from "preact/compat";
import {useMediaQuery} from "@mui/material";
import {useTheme} from "@mui/material/styles";
import SearchBar from "material-ui-search-bar";
import ScrollableBox, {useDefaultLipClassNames} from 'react-scrollable-box';
import 'react-scrollable-box/lib/default.css';

export default function Search() {

    const theme = useTheme()
    const lipClassNames = useDefaultLipClassNames();

    const [searchPhrase, setSearchPhrase] = useState("")
    const [items, setItems] = useState([])
    const [users, setUsers] = useState([])

    useEffect(() => {
        const fetchItems = async () => {
            const items = await fetch(`http://localhost:8080/items${searchPhrase !== "" ? `?search=${searchPhrase}` : ""}`, {
                credentials: 'include'
            })
                .then(r => r.json())
                .then(r => Array.isArray(r) ? r : [r])
            setItems(items)
            return items
        }
        fetchItems().then(r => console.log("Fetched items:", r));
    }, [searchPhrase])

    useEffect(() => {
        const fetchUsers = async () => {
            const newUsers = await Promise.all(items
                .filter(({owner}) => !users.find(({id}) => id === owner))
                .map(async ({owner}) => {
                    return await fetch(`http://localhost:8080/users/${owner}`, {
                        credentials: 'include'
                    })
                        .then(r => r.json())
                }))
            setUsers([...users, ...newUsers])
            return users
        }
        fetchUsers().then(r => console.log("Fetched users:", r));
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
            {/*<SearchBox onChange={setSearchPhrase}/>*/}
            <SearchBar
                value={searchPhrase}
                onChange={setSearchPhrase}
                onCancelSearch={() => setSearchPhrase("")}
                // onRequestSearch={() => doSomethingWith(this.state.value)}
            />
        </Container>
        <Container sx={{mt: 4, mb: 4}}>
            <Container>
                <Grid
                    container
                    spacing={{xs: 2, md: 3}}
                    columns={{xs: 2, sm: 8, md: 12}}
                >
                    {items.map(item =>
                        <Grid item key={item.id} xs={2} sm={4} md={4} display="flex" flexDirection={'column'}
                              alignItems="center">
                            <SingleItem item={item} owner={users.find(({id}) => item.owner === id)}/>
                        </Grid>
                    )}
                </Grid>
            </Container>
            <Copyright sx={{pt: 4}}/>
        </Container>
    </ScrollableBox>)
}
