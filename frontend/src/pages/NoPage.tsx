import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import Container from "@mui/material/Container";
import Grid from "@mui/material/Grid";
import Paper from "@mui/material/Paper";
import Chart from "../components/Chart";
import {Copyright} from "../components/Copyright";
import Typography from "@mui/material/Typography";
import SentimentVeryDissatisfiedIcon from '@mui/icons-material/SentimentVeryDissatisfied';
import {Link} from "react-router-dom";

export default function NoPage() {
    return (<Box
        component="main"
        sx={{
            backgroundColor: (theme) =>
                theme.palette.mode === 'light'
                    ? theme.palette.grey[100]
                    : theme.palette.grey[900],
            flexGrow: 1,
            height: '100vh',
            overflow: 'auto',
        }}
    >
        <Toolbar/>
        <Container maxWidth="lg" sx={{mt: 4, mb: 4}}>
            <Link to={"/"}>
                <Box sx={{display: "flex", justifyContent: "center", alignItems: "center", padding: 10}}>
                    <SentimentVeryDissatisfiedIcon/>
                    <Typography
                        component="h1"
                        variant="h6"
                        color="inherit"
                        noWrap>Return Back Home</Typography>
                </Box>
            </Link>
            <Copyright sx={{pt: 4}}/>
        </Container>
    </Box>)
}
