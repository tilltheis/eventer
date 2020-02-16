import React, { useState } from 'react';
import './App.css';
import Container from 'react-bootstrap/Container';
import { BrowserRouter as Router, Switch, Route } from 'react-router-dom';
import uuid from 'uuid';
import Navbar from 'react-bootstrap/Navbar';
import Nav from 'react-bootstrap/Nav';
import Toast from './Toast';
import EventEditor from './EventEditor';
import EventList from './EventList';
import UserSession from './UserSession';
import EventRepository from './EventRepository';
import UserSessionRepository from './UserSessionRepository';
import { getCookieValue } from './utils';

export default function App() {
  const [toast, setToast] = useState(undefined);
  const eventRepository = new EventRepository();
  const userSessionRepository = new UserSessionRepository();
  const getLoggedInUser = () => {
    const userCookie = getCookieValue('jwt-header.payload');
    return userCookie !== null ? JSON.parse(atob(userCookie.split('.')[1])) : null;
  };

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
                <UserSession userSessionRepository={userSessionRepository} getLoggedInUser={getLoggedInUser} />
              </Navbar.Text>
            </Navbar.Collapse>
          </Navbar>
        </header>
        <main>
          <Container style={{ position: 'relative' }}>
            {toast && <Toast header={toast.header} body={toast.body} />}

            <Switch>
              <Route path="/events/new">
                <EventEditor generateUuid={uuid.v4} setToast={setToast} eventRepository={eventRepository} />
              </Route>
              <Route path="/">
                <EventList eventRepository={eventRepository} />
              </Route>
            </Switch>
          </Container>
        </main>
      </Container>
    </Router>
  );
}
