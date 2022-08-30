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
// noinspection TypeScriptCheckImport
import ModalImage from 'react-modal-image';
import {Button} from "@mui/material";
import Link from "@mui/material/Link";
import {useNavigate} from "react-router-dom";

export default function SingleItem(props) {

    const {item, owner, minMaxWidth} = props;
    const navigate = useNavigate()

    return (
        <Card sx={{maxWidth: (minMaxWidth ? minMaxWidth[1] : 340), minWidth: (minMaxWidth ? minMaxWidth[0] : 300),}}>
            <CardHeader
                avatar={
                    owner
                        ? <Avatar src={owner.avatarUrl} alt={"Owner Image"} sx={{width: 36, height: 36}}/>
                        : <Avatar sx={{bgcolor: red[500]}} aria-label="recipe">A</Avatar>
                }
                // action={
                //     <IconButton aria-label="settings">
                //         <MoreVertIcon />
                //     </IconButton>
                // }
                title={item.title}
                subheader={`By ${owner ? owner.name : "anonymous"}`}
            />
            <div style={{display: "flex", justifyContent: "center"}}>
                <CardMedia
                    component={ModalImage}
                    sx={{height: "194px"}}
                    small={item.image}
                    medium={item.image}
                    hideDownload={true}
                    hideZoom={true}
                    alt={item.title}
                />
            </div>
            <CardContent>
                <Typography variant="body2" color="text.secondary">
                    {item.description}
                </Typography>
            </CardContent>
            <CardActions disableSpacing sx={{justifyContent: "flex-end"}}>
                <Button
                    onClick={() => {
                        if (1 < item.auctions?.length) {
                            navigate(`/auctions?items=${item.id}`)
                        } else if (1 === item.auctions?.length) {
                            navigate(`/auctions/${item.auctions[0]}`)
                        }
                    }}
                    size="small" color="primary"
                    sx={{justifyContent: "center"}}>
                    Go To Auction
                </Button>
                <IconButton aria-label="share" sx={{alignSelf: "right"}}>
                    <ShareIcon/>
                </IconButton>
            </CardActions>
        </Card>
    );
}
