import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import Container from "@mui/material/Container";
import Grid from "@mui/material/Grid";
import {Copyright} from "../components/Copyright";
import SingleItem from "../components/SingleItem";
import {useEffect, useState} from "preact/compat";
import {Button, Chip, TextField, useMediaQuery} from "@mui/material";
import {useTheme} from "@mui/material/styles";
import SearchBar from "material-ui-search-bar";
import {Link, useNavigate, useParams} from "react-router-dom";
import CardHeader from "@mui/material/CardHeader";
import Avatar from "@mui/material/Avatar";
import {red} from "@mui/material/colors";
import CardMedia from "@mui/material/CardMedia";
import CardContent from "@mui/material/CardContent";
import Typography from "@mui/material/Typography";
import CardActions from "@mui/material/CardActions";
import IconButton from "@mui/material/IconButton";
import ChevronLeftIcon from "@mui/icons-material/ChevronLeft";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import PlusBoxIcon from '@mui/icons-material/AddBox';
import MinusBoxIcon from '@mui/icons-material/IndeterminateCheckBox';
import AssignmentTurnedIn from "@mui/icons-material/AssignmentTurnedIn";
import Card from "@mui/material/Card";
import ScrollableBox, {useDefaultLipClassNames} from "react-scrollable-box";
import flatten from "lodash/flatten"
import max from "lodash/max";
import Divider from "@mui/material/Divider";
import Paper from "@mui/material/Paper";
// import ImageGallery from 'react-image-gallery';
// import "react-image-gallery/styles/css/image-gallery.css";
import {CarouselProvider, Slider, Slide, ButtonBack, ButtonNext, Image} from 'pure-react-carousel';
import 'pure-react-carousel/dist/react-carousel.es.css';
import CurrencyInput from 'react-currency-input-field';
import {VirtualizedTable} from "../components/VirtualizedTable";

export default function Auction() {

    const theme = useTheme()
    const lipClassNames = useDefaultLipClassNames();
    const {auctionId} = useParams()
    const navigate = useNavigate()

    const [auction, setAuction] = useState(undefined)
    const [items, setItems] = useState([])
    const [bids, setBids] = useState([])
    const realTimeBids = []
    const [secondsLeft, setSecondsLeft] = useState(0)
    const [currentPrice, setCurrentPrice] = useState(0.0)
    const [newBidPrice, setNewBidPrice] = useState(0.0)

    useEffect(() => {
        if (currentPrice >= newBidPrice) {
            setNewBidPrice(currentPrice + 1.0)
        }
    }, [currentPrice])

    useEffect(() => {
        const fetchAuction = async () => {
            try {
                const auction = await fetch(`http://localhost:8080/auctions/${auctionId}`, {
                    credentials: 'include'
                })
                    .then(r => r.json())
                setAuction({success: auction})

                const currentPrice = auction.bids.length
                    ? max(auction.bids.map(bid => bid.amount))
                    : auction?.lowestBid?.amount;
                setCurrentPrice(currentPrice as number);

                const secondsLeft = (new Date(auction.end).getTime() - new Date().getTime()) / 1000 | 0
                setSecondsLeft(secondsLeft)
                setInterval(() => {
                    setSecondsLeft(seconds => --seconds);
                }, 1000);

                realTimeBids.push(...auction.bids)
                setBids(realTimeBids)

                const websocket = new WebSocket(`ws://localhost:8083/auctions/${auctionId}/newBids`)
                websocket.onmessage = (event) => {
                    const newBids = JSON.parse(event.data);
                    realTimeBids.unshift(...newBids)
                    setBids(realTimeBids)
                    setCurrentPrice(max(realTimeBids.map(bid => bid.amount)))
                }
                websocket.onopen = (event) => {
                    console.log('connected');
                };
                websocket.onclose = () => {
                    console.log('disconnected')
                    // automatically try to reconnect on connection loss
                }
                websocket.onerror = err => {
                    console.error("Socket encountered error: ", err);
                    websocket.close();
                };

                return auction
            } catch (e) {
                console.error("Error when fetching auction with id " + auctionId, e)
                setAuction({failure: e})
            }
        }
        fetchAuction().then(r => console.log("Fetched auction:", r));
    }, [])

    useEffect(() => {
        const fetchItems = async () => {
            const itemIds = auction?.success?.items || [];
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
    }, [auction])

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
            {
                auction
                    ? (auction.success
                        ? <>
                            <Grid
                                container
                                mt={2}
                                spacing={12}
                                sx={{justifyContent: "center"}}
                            >
                                <Grid
                                    item
                                    sm={6}
                                    md={4}
                                >
                                    <div
                                        style={{display: "flex", textAlign: "center", maxHeight: "40vh", minWidth: "40vh"}}>
                                        {
                                            items.length
                                                ? <CarouselProvider
                                                    style={{width: "100%"}}
                                                    naturalSlideWidth={200}
                                                    naturalSlideHeight={200}
                                                    visibleSlides={1}
                                                    totalSlides={2}
                                                    hasMasterSpinner>
                                                    <Slider>
                                                        {
                                                            items.map(({image}, index) => (
                                                                <Slide index={index}>
                                                                    <Image src={image} style={{objectFit: "contain"}}/>
                                                                </Slide>))
                                                        }
                                                    </Slider>
                                                    {items.length > 1 ? <ButtonBack><ChevronLeftIcon/></ButtonBack> : null}
                                                    {items.length > 1 ? <ButtonNext><ChevronRightIcon/></ButtonNext> : null}
                                                </CarouselProvider>
                                                : <img
                                                    src="https://upload.wikimedia.org/wikipedia/commons/6/62/ImagePlaceholder_icon.svg"/>
                                        }
                                    </div>

                                    <div style={{display: "flex", textAlign: "center", minWidth: "40vh", marginTop: "40%"}}>
                                        <Typography component='h3' variant='h3' gutterBottom sx={{color: "red"}}>
                                            {secondsLeft > 0 ? `Time Left: ${secondsLeft} seconds` : 'Auction ended'}
                                        </Typography>
                                    </div>
                                </Grid>

                                <Grid item xs={12} sm={6} md={8}>
                                    <Typography component='h2' variant='h2' textAlign='start' gutterBottom>
                                        {auction.success.title}
                                    </Typography>

                                    {auction.success?.keywords?.map(keyword => <Chip label={keyword} sx={{
                                        margin: "0.3em",
                                        marginBottom: "1em"
                                    }}/>)}

                                    <Divider/>

                                    <Typography variant='caption' component='p' sx={{padding: "5%"}}>
                                        {auction.success.description}
                                    </Typography>

                                    <Divider sx={{mb: 2}}/>

                                    <Grid
                                        container
                                        mt={2}
                                        sx={{textAlign: "center"}}
                                    >
                                        <Grid
                                            item
                                            xs={12} sm={12} md={6}
                                            direction={"column"} sx={{mb: '2em'}}
                                        >
                                            <Typography component='h5' variant='h6'>
                                                Current Price: {currentPrice}€
                                            </Typography>

                                            <Typography component='p' variant='caption'>
                                                Lowest Bid: {auction?.success?.lowestBid?.amount || 0}€
                                            </Typography>
                                        </Grid>

                                        <Grid
                                            item
                                            xs={12} sm={12} md={6}
                                            sx={{mb: '2em'}}
                                        >
                                            <IconButton onClick={() => setNewBidPrice(newBidPrice - 1)}
                                                        sx={{height: "100%"}}>
                                                <MinusBoxIcon/>
                                            </IconButton>
                                            <CurrencyInput
                                                value={newBidPrice}
                                                placeholder="Your New Bid"
                                                decimalsScale={2}
                                                intlConfig={{locale: "en-GB", currency: "EUR"}}
                                                decimalSeparator={"."}
                                                onValueChange={(value) => setNewBidPrice(Number.parseFloat(value))}
                                                // customInput={TextField}
                                                // variant="outlined"
                                                // label="Your New Bid"
                                            />
                                            <IconButton onClick={() => setNewBidPrice(newBidPrice + 1)}
                                                        sx={{height: "100%"}}>
                                                <PlusBoxIcon/>
                                            </IconButton>
                                        </Grid>
                                    </Grid>

                                    <Box display='flex' justifyContent={'center'}>

                                        <Button
                                            variant='contained'
                                            color='success'
                                            startIcon={<AssignmentTurnedIn/>}
                                            onClick={() => {
                                                fetch(`http://localhost:8080/auctions/${auctionId}/bidding`, {
                                                    method: "POST",
                                                    body: JSON.stringify({
                                                        amount: newBidPrice,
                                                        currency: 'EUR'
                                                    }),
                                                    headers: {
                                                        "Content-Type": "application/json"
                                                    },
                                                    credentials: 'include'
                                                })
                                                    .then(() => console.log(`Successfully placed new bid.`))
                                                    .catch((e) => console.error(`Couldn't place new bid: `, e))
                                            }}
                                        >
                                            Place Bid
                                        </Button>
                                    </Box>

                                    <Divider sx={{mt: 2}}/>

                                    <Paper style={{height: 400, width: '100%'}}>
                                        <VirtualizedTable
                                            rowCount={bids.length}
                                            rowGetter={({index}) => bids[index]}
                                            columns={[
                                                {
                                                    width: 200,
                                                    label: 'Price',
                                                    dataKey: 'amount',
                                                    numeric: true,
                                                },
                                                {
                                                    width: 150,
                                                    label: 'Currency',
                                                    dataKey: 'currency',
                                                },
                                                {
                                                    width: 650,
                                                    label: 'Bidder',
                                                    dataKey: 'bidder',
                                                },
                                                {
                                                    width: 460,
                                                    label: 'Timestamp',
                                                    dataKey: 'timestamp',
                                                },
                                            ]}
                                        />
                                    </Paper>
                                </Grid>
                            </Grid>

                        </>
                        : <Typography variant="h2" color="text.secondary">
                            Couldn't find auction with ID {auctionId}...
                        </Typography>)
                    : <div></div>
            }
            <Copyright sx={{pt: 4}}/>
        </Container>
    </ScrollableBox>)
}
