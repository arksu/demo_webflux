import React from 'react';
import ReactDOM from 'react-dom/client';
import {
    createBrowserRouter,
    RouterProvider,
} from "react-router-dom";
import reportWebVitals from './reportWebVitals';

import 'bootstrap/dist/css/bootstrap.min.css';
import 'react-toastify/dist/ReactToastify.css';
import './index.css';

import ErrorPage from "./ErrorPage";
import Widget from "./pages/Widget";
import {ToastContainer} from "react-toastify";

const router = createBrowserRouter([
        {
            path: "/",
            element: <ErrorPage/>
        },
        {
            path: "/invoice",
            element: <ErrorPage/>
        },
        {
            path: "/:id",
            element: <Widget/>,
            errorElement: < ErrorPage/>,
        },
        {
            path: "/invoice/:id",
            element: <Widget/>,
            errorElement: < ErrorPage/>,
        }
    ])
;

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
    <React.StrictMode>
        <ToastContainer/>
        <RouterProvider router={router}/>
    </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
