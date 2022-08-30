import * as React from 'react';
import {styled} from '@mui/material/styles';
import Card from '@mui/material/Card';
import CardHeader from '@mui/material/CardHeader';
import CardMedia from '@mui/material/CardMedia';
import CardContent from '@mui/material/CardContent';
import CardActions from '@mui/material/CardActions';
import Collapse from '@mui/material/Collapse';
import Avatar from '@mui/material/Avatar';
import IconButton, {IconButtonProps} from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import {red} from '@mui/material/colors';
import FavoriteIcon from '@mui/icons-material/Favorite';
import ShareIcon from '@mui/icons-material/Share';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import {Button, CardActionArea, ImageList, ImageListItem, ImageListItemBar} from "@mui/material";
import {useNavigate} from "react-router-dom";
import max from "lodash/max";

export default function SingleAuction(props) {

    const {auction, items, seller, minMaxWidth} = props;
    const navigate = useNavigate()

    return (
        <Card sx={{maxWidth: (minMaxWidth ? minMaxWidth[1] : 340), minWidth: (minMaxWidth ? minMaxWidth[0] : 300),}}>
            <CardActionArea onClick={() => navigate(`/auctions/${auction.id}`)}>
                <CardHeader
                    avatar={
                        seller
                            ? <Avatar src={seller.avatarUrl} alt={"Owner Image"} sx={{width: 36, height: 36}}/>
                            : <Avatar sx={{bgcolor: red[500]}} aria-label="recipe">A</Avatar>
                    }
                    // action={
                    //     <IconButton aria-label="settings">
                    //         <MoreVertIcon />
                    //     </IconButton>
                    // }
                    title={auction.title}
                    subheader={`By ${seller ? seller.name : "anonymous"}`}
                />
                <div style={{display: "flex", justifyContent: "center"}}>
                    <ImageList
                        variant="quilted"
                        cols={2}
                        gap={4}>
                        {items.map((item) => (
                            <ImageListItem key={item.image}>
                                <CardMedia
                                    component="img"
                                    height={200}
                                    src={item.image}
                                    srcSet={`${item.image} 2x`}
                                    alt={item.title}
                                    loading="lazy"
                                />
                            </ImageListItem>
                        ))}
                    </ImageList>
                </div>
                <CardContent>
                    <Typography variant="body2" color="text.secondary">
                        {auction.description}
                    </Typography>
                </CardContent>
            </CardActionArea>
            <CardActions disableSpacing sx={{padding: "8px 30px 8px 30px"}}>
                <Typography variant="h6" color="rgb(240,102,102)">
                    {auction.bids ? `${max(auction.bids.map(bid => bid.amount))}€` : auction?.lowestBid?.amount}
                </Typography>
                <Button onClick={() => navigate(`/auctions/${auction.id}`)} size="small" color="primary"
                        sx={{justifyContent: "center", marginLeft: "auto"}}>
                    Details
                </Button>
                {/*<IconButton aria-label="share" sx={{alignSelf: "right"}}>*/}
                {/*    <ShareIcon/>*/}
                {/*</IconButton>*/}
            </CardActions>
        </Card>
    );
}
