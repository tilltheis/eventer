import React from 'react';
import logo from './logo.svg';
import './App.css';
import Container from 'react-bootstrap/Container';
import EventList from './EventList';

export default function App() {
  return (
    <Container>
      <header>
        <h1 className="display-1"><a href="/">Eventer</a></h1>
      </header>
      <main>
        <Container>
          <EventList/>
        </Container>
      </main>
    </Container>
  );
}

