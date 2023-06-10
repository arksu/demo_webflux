import React from 'react';
import ReactDOM from 'react-dom/client';
import {
    createBrowserRouter,
    RouterProvider,
} from "react-router-dom";
import {ToastContainer} from "react-toastify";
import {I18nextProvider} from "react-i18next";

// import reportWebVitals from './reportWebVitals';

import 'bootstrap/dist/css/bootstrap.min.css';

import 'react-toastify/dist/ReactToastify.css';
import './index.css';

import ErrorPage from "./ErrorPage";
import Widget from "./pages/Widget";
import i18n from "./i18n";
import {Col, Container, Row} from "react-bootstrap";

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
    // <React.StrictMode>
    <I18nextProvider i18n={i18n}>
        <ToastContainer/>
        <div className="main">
            <Container>
                <Row className="justify-content-center">
                    <Col xs={9} sm={8} md={6} lg={5} xl={4} xxl={3}
                         className="shadow-lg border border-primary widget-container-row text-center">
                        <RouterProvider router={router}/>
                    </Col>
                </Row>
            </Container>
        </div>
    </I18nextProvider>
    // </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
// reportWebVitals();
