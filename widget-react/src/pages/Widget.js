import {useParams} from "react-router";
import {toast} from "react-toastify";
import {Button} from "react-bootstrap";

const action = () => {
    toast.error('kdsjflkds', {
        autoClose: 2000,
        hideProgressBar: false,
        closeOnClick: true,
        pauseOnHover: true,
        draggable: true,
        theme: "colored",
    });
}

function Widget() {
    let {id} = useParams()
    return (
        <div>
            <h1>widget id: {id} </h1>
            <Button onClick={action}>Some Action</Button>
        </div>
    )
}

export default Widget
