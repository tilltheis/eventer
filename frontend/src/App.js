import React, { useState } from 'react';
import logo from './logo.svg';
import './App.css';
import Container from 'react-bootstrap/Container';
import EventList from './EventList';
import EventEditor from './EventEditor';
import {
  BrowserRouter as Router,
  Switch,
  Route
} from "react-router-dom";
import uuid from 'uuid';
import Toast from './Toast';
import Navbar from 'react-bootstrap/Navbar'
import Nav from 'react-bootstrap/Nav'
import UserSession from './UserSession';

export default function App() {
  const [toast, setToast] = useState(undefined);

  return (
    <Router>
      <Container>
        <header>
        <Navbar bg="light">
          <Navbar.Brand href="/">Eventer</Navbar.Brand>
          <Nav.Link href="/events/new">Create New Event</Nav.Link>
          <Navbar.Toggle />
          <Navbar.Collapse className="justify-content-end">
            <Navbar.Text>
              <UserSession />
            </Navbar.Text>
          </Navbar.Collapse>
        </Navbar>
        </header>
        <main>
          <Container style={{ position: 'relative' }}>
            {toast && <Toast {...toast} />}

            <Switch>
              <Route path="/events/new">
                <EventEditor generateUuid={uuid.v4} setToast={setToast} />
              </Route>
              <Route path="/">
                <EventList />
              </Route>
            </Switch>
          </Container>
        </main>
      </Container>
    </Router>
  );
};